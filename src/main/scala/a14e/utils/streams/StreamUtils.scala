package a14e.utils.streams

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import a14e.utils.concurrent.FutureImplicits
import a14e.utils.concurrent.FutureImplicits._
import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.{FlowShape, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source, Zip}
import com.typesafe.scalalogging.LazyLogging
import io.circe._

import scala.collection.{SeqLike, immutable}
import scala.collection.concurrent.TrieMap
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object StreamUtils extends LazyLogging {


  /**
    * берет элементы только под номером n
    **/
  def takeEachNth[T](n: Int): Flow[T, T, _] = {
    Flow[T].statefulMapConcat { () =>
      var counter = -1
      x =>
        counter += 1
        if (counter % n == 0) {
          counter = 0
          x :: Nil
        } else Nil
    }
  }

  // отличается тем, что прогонит весь поток, чтобы явно прочитать все входящие данные из потока
  def exists[T](f: T => Boolean): Sink[T, Future[Boolean]] = {
    Sink.fold[Boolean, T](false)((prev, curr) => prev || f(curr))
  }

  def existsFast[T](f: T => Boolean): Sink[T, Future[Boolean]] = {

    find(f).mapMaterializedValue(_.map(_.nonEmpty)(FutureImplicits.sameThreadExecutionContext))
  }

  def find[T](f: T => Boolean): Sink[T, Future[Option[T]]] = {
    Flow[T].dropWhile(x => !f(x)).toMat(Sink.headOption)(Keep.right)
  }


  def filterOpt[A, B](valueOpt: Option[B])(test: (A, B) => Boolean): Flow[A, A, _] = {
    valueOpt match {
      case None => Flow[A]
      case Some(value) => Flow[A].filter(test(_, value))
    }
  }

  // logging - сделать имплиситом?
  def collectSuccess[T](failOrErr: Boolean = false): Flow[Try[T], T, _] = Flow[Try[T]].mapConcat {
    case Success(x) => x :: Nil
    case Failure(err) =>
      logger.error("Received error", err)
      if (failOrErr) throw err
      Nil
  }

  def mapAsyncSuccess[T, B](parallelLevel: Int,
                            failOrErr: Boolean = false)(f: T => Future[B]): Flow[T, B, _] = {
    Flow[T].mapAsync(parallelLevel)(x => f(x).extractTry)
      .via(collectSuccess(failOrErr))
  }

  // TODO тесты
  def filterAsync[T](parallelLevel: Int,
                     failOrErr: Boolean = false)
                    (func: T => Future[Boolean]): Flow[T, T, _] = {

    val mapAsyncFlow = mapAsyncSuccess[T, (Boolean, T)](parallelLevel, failOrErr) { x =>
      func(x).map(testResult => (testResult, x))(FutureImplicits.sameThreadExecutionContext)
    }

    Flow[T].via(mapAsyncFlow).collect { case (true, x) => x }
  }


  // TODO тесты
  def mapConcatAsync[T, B](func: T => Future[immutable.Seq[B]],
                           parallelLevel: Int = 1,
                           failOrErr: Boolean = false): Flow[T, B, _] = {
    Flow[T]
      .via(mapAsyncSuccess(parallelLevel, failOrErr)(func))
      .mapConcat(x => x)
  }



  def viaSecondOfTuple[IN1, IN2, OUT](subFlow: Flow[IN2, OUT, _]): Flow[(IN1, IN2), (IN1, OUT), _] = {

    val graph = GraphDSL.create() { implicit b =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      // prepare graph elements
      val broadcast = b.add(Broadcast[(IN1, IN2)](2))
      val merge = b.add(Zip[IN1, OUT]())


      broadcast.out(0) ~> firstOfTuple[IN1]() ~> merge.in0
      broadcast.out(1) ~> secondOfTuple[IN2]() ~> subFlow ~> merge.in1

      // expose ports
      FlowShape(broadcast.in, merge.out)
    }

    Flow.fromGraph(graph)
  }



  def viaFirstOfTuple[IN1, IN2, OUT](subFlow: Flow[IN1, OUT, _]): Flow[(IN1, IN2), (OUT, IN2), _] = {
    Flow[(IN1, IN2)]
      .via(rotateTuple())
      .via(viaSecondOfTuple(subFlow))
      .via(rotateTuple())
  }

  def parallelFlow[IN, OUT](subFlow: Flow[IN, OUT, _]): Flow[IN, (IN, OUT), _] = {
    val graph = GraphDSL.create() { implicit b =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      // prepare graph elements
      val broadcast = b.add(Broadcast[IN](2))
      val merge = b.add(Zip[IN, OUT]())


      broadcast.out(0) ~> merge.in0
      broadcast.out(1) ~> subFlow ~> merge.in1

      // expose ports
      FlowShape(broadcast.in, merge.out)
    }

    Flow.fromGraph(graph)
  }


  def firstOfTuple[A](): Flow[(A, _), A, _] = Flow[(A, Any)].map { case (a, _) => a }

  def secondOfTuple[B](): Flow[(_, B), B, _] = Flow[(Any, B)].map { case (_, b) => b }

  def zipWithTail[T <: AnyRef]: Flow[T, (T, T), _] = Flow[T].statefulMapConcat { () =>
    var last = null.asInstanceOf[T]
    el =>

      if (last ne null) {
        val res = (last, el)
        last = el
        res :: Nil
      } else {
        last = el
        Nil
      }
  }

  def rotateTuple[A, B](): Flow[(A, B), (B, A), _] = Flow[(A, B)].map { case (a, b) => (b, a) }


  // TODO тесты
  /** работает для flow из коллекций
    *
    * пропускает вперед только коллекции с суммарным размером не больше maxSize,
    * остальные эллементы не пускает дальше, на последнем элементе делается slice
    *
    *
    * импользуется при обработке сообщени из websocket'а
    * чтобы не было переполнения памяти
    *
    *
    * не умеет работать с бесконечными коллекциями
    * */
  def limitBySizeFlow[T, A](maxSize: Int)
                           (implicit
                            conv: T => SeqLike[A, T],
                            builder: CanBuildFrom[_, A, T]): Flow[T, T, _] = {
    assert(maxSize > 0, "Limit of received bytes should be positive")

    Flow[T].statefulMapConcat { () =>
      var receiverBytes: Int = 0
      var closed = false

      inputData =>
        if (closed) Nil
        else {
          val newSize = receiverBytes + inputData.size
          if (newSize <= maxSize) {
            receiverBytes = newSize
            inputData :: Nil
          } else {
            closed = true
            val lastBatchSize = maxSize - receiverBytes
            if (lastBatchSize <= 0) {
              Nil
            } else {
              val lastBatch = inputData.take(lastBatchSize)
              lastBatch :: Nil
            }
          }
        }
    }
  }

  def parsingWsJsonFlow[T: Decoder](maxBytesSize: Int = 1024 * 1024) // по умолчанию 1 мегабайт
                                   (implicit
                                    executionContext: ExecutionContext,
                                    materializer: Materializer): Flow[Message, T, _] = {
    Flow[Message]
      .via(limitedWsTextMessageFlow(maxBytesSize))
      .via(circeDecodeFlow[T])

  }

  /** Извлекает из потока сообщений вебсокета только текстовые сообщения,
    * в том числе потоковы, а для них обрубает лишнюю длинну (если больше maxBytesSize)
    *
    * в случае ошибки -- логируется, но продолжает работать
    * */
  def limitedWsTextMessageFlow(maxBytesSize: Int = 1024 * 1024) // по умолчанию 1 мегабайт
                              (implicit
                               executionContext: ExecutionContext,
                               materializer: Materializer): Flow[Message, String, _] = {
    Flow[Message].mapConcat {
      case _: BinaryMessage =>
        logger.warn("Received binary message. Cant handle, ignoring")
        Nil
      case t: TextMessage =>
        val limitedStream = t.textStream.via(limitBySizeFlow(maxBytesSize))
        TextMessage(limitedStream) :: Nil
    }.mapAsync(1) { message =>
      message.textStream.runFold(new StringBuilder()) {
        (builder, textPart) => builder ++= textPart
      }.map(_.result())
        .extractTry
    }.mapConcat {
      case Success(s) => s :: Nil
      case Failure(f) =>
        logger.warn("Materializing text stream failed with error", f)
        Nil
    }
  }


  def circeDecodeFlow[T: Decoder]: Flow[String, T, _] = {
    // TODO унифицировать обработку ошибок?
    Flow[String].mapConcat { text =>
      val decoded = for {
        json <- parser.parse(text)
        decoded <- json.as[T]
      } yield decoded

      decoded match {
        case Right(x) => x :: Nil
        case Left(error) =>
          logger.warn("Decoding json failed with error", error)
          Nil
      }

    }
  }


  /** TODO подумать, как тут делать тесты */
  /**
    * Сложная фигня, но необходима.
    *
    * Она автоматически начинает слушать соединение и
    * перезапускает сокет в случае необходимости
    **/
  def webSocketSource(uri: Uri,
                      bufferSize: Int = 1024,
                      overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead,
                      autorestart: Boolean = true)
                     (implicit
                      marializer: Materializer,
                      executionContext: ExecutionContext,
                      actorSystem: ActorSystem): Source[Message, _] = {


    def initSource(): Source[Message, _] = {
      logger.info(s"Initializing ws to $uri")

      val (queue, publisher) =
        Source.queue[Message](bufferSize, OverflowStrategy.dropHead)
          .toMat(Sink.asPublisher(false))(Keep.both)
          .run()

      val queueClosed = new AtomicBoolean(false)

      val connectionsCounter = new AtomicLong(0)
      val actionsOnClose = TrieMap[Long, () => Unit]()

      queue.watchCompletion().andThen {
        case Success(_) =>
          logger.warn(s"Ws $uri queue closed")
        case Failure(e) =>
          logger.warn(s"Ws $uri queue closed with error", e)
      }.onComplete { _ =>
        actionsOnClose.values.foreach(func => func())
        queueClosed.set(false)
      }


      def initListening(): Unit = {
        val restarted = new AtomicBoolean(false)


        val sourceEmpty: Source[Message, Promise[Option[Message]]] = Source.maybe[Message]
        val toQueueSink: Sink[Message, Future[Done]] = Sink.foreach[Message](x => queue.offer(x))
        val flow: Flow[Message, Message, (Future[Done], Promise[Option[Message]])] =
          Flow.fromSinkAndSourceMat(toQueueSink, sourceEmpty)(Keep.both)

        /**
          * close -- выполнится когда сервер закроется
          * closePromise -- для того, чтобы закрыть самостоятельно
          **/
        val (upgradeResponse, (closed, closePromise)) =
          Http().singleWebSocketRequest(WebSocketRequest(uri), flow)

        val connected = upgradeResponse.map { upgrade =>
          logger.info("Upgrade response: " + upgradeResponse)
          // just like a regular http request we can access response status which is available via upgrade.response.status
          // status code 101 (Switching Protocols) indicates that server support WebSockets
          if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
            Done
          } else {
            throw new RuntimeException(s"Connecting to ws $uri failed: ${upgrade.response.status}")
          }
        }

        def tryRestart(): Unit = if (autorestart && !restarted.getAndSet(true) && !queueClosed.get()) {
          logger.info(s"restarting ws to $uri")
          if (!closePromise.isCompleted)
            closePromise.success(None)

          Source.tick(1.seconds, 1.seconds, (): Unit)
            .take(1)
            .runForeach { _ =>
              initListening()
            }
        }

        // чтобы избежать переполнения стека
        val action: () => Unit = {
          () =>
            logger.info("queue completed. Closing socket")
            if (!closePromise.isCompleted)
              closePromise.success(None)
        }
        val conId = connectionsCounter.incrementAndGet()
        if (!queue.watchCompletion().isCompleted) {
          actionsOnClose += (conId -> action)
        } else {
          if (!closePromise.isCompleted)
            closePromise.success(None)
        }
        closePromise.future.onComplete { _ =>
          actionsOnClose -= conId
        }


        connected.andThen {
          case Failure(e) =>
            logger.warn(s"Connecting to ws $uri failed with error", e)
            tryRestart()
        }

        closed.andThen {
          case Success(_) =>
            logger.info(s"Connection to ws $uri closed")
          case Failure(e) =>
            logger.warn(s"Connecting to ws $uri failed with error", e)
        }.onComplete {
          _ => tryRestart()
        }

      }

      initListening()

      Source.fromPublisher(publisher)
    }


    Source.lazily(() => initSource())
  }
}

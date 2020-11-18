package a14e.commons.camundadsl

import a14e.commons.camundadsl.TopicRoute.TopicBuilder
import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.catseffect.CatsIoImplicits._
import a14e.commons.context.{Contextual, LazyContextLogging}
import cats.arrow.FunctionK
import cats.effect.concurrent.{MVar, MVar2}
import cats.{Traverse, ~>}
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Effect, IO, Sync, Timer}
import com.typesafe.scalalogging.LazyLogging
import org.camunda.bpm.client.ExternalTaskClient
import org.camunda.bpm.client.topic.TopicSubscription

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.higherKinds


class TopicRoute[F[_]](val routes: List[CamundaSubscription[F]]) extends LazyContextLogging {

  import cats.implicits._
  import cats.effect.implicits._

  def ++(other: TopicRoute[F]): TopicRoute[F] = new TopicRoute[F](this.routes ++ other.routes)


  type Topic = String

  def run(buildClient: => F[ExternalTaskClient])(implicit
                                                 shift: ContextShift[F],
                                                 sync: ConcurrentEffect[F],
                                                 contextual: Contextual[F]): F[Seq[TopicSubscription]] = {

    def getClientCached(mvar: MVar2[F, Map[String, ExternalTaskClient]],
                        name: String): F[ExternalTaskClient] = {
      mvar.modify { clients =>
        clients.get(name) match {
          case None =>
            Sync[F].defer(buildClient).map { client =>
              val newClients = clients + (name -> client)
              newClients -> client
            }
          case Some(client) =>
            Sync[F].pure(clients -> client)
        }
      }
    }
    import fs2.Stream

    (for {
      mvar <- Stream.eval(MVar.of(Map.empty[String, ExternalTaskClient]))
      commonClient <- Stream.eval(buildClient)
      subscription <- Stream.iterable(routes)

      client <- Stream.eval {
        subscription.differentLoop
          .map(getClientCached(mvar, _))
          .getOrElse(Sync[F].pure(commonClient))
      }
      result <- Stream.eval(subscription.run(client))
    } yield result)
      .compile
      .toVector
      .map(_.toSeq)

  }

}

object TopicRoute {
  def build[F[_]](processDefKey: String)
                 (builder: TopicBuilder[F] => Unit): TopicRoute[F] = {
    val topicBuilder = new TopicBuilder[F](processDefKey)
    builder(topicBuilder)
    topicBuilder.build
  }

  class TopicBuilder[F[_]](val processDefKey: String) {
    def +=(s: CamundaSubscription[F]): TopicBuilder[F] = {
      buffer += s
      this
    }

    def build: TopicRoute[F] = new TopicRoute[F](buffer.result())

    private val buffer = new ListBuffer[CamundaSubscription[F]]
  }

  object TopicBuilder {
    def apply[F[_] : TopicBuilder]: TopicBuilder[F] = implicitly[TopicBuilder[F]]
  }

}

object Routes extends LazyLogging {

  import cats.implicits._

  def route[
    F[_] : TopicBuilder: Sync,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      bpmnErrors: Boolean = false,
                      sendDiffOnly: Boolean = true,
                      maxParallelism: Int = 2,
                      differentLoop: Option[String] = None)
                     (handle: IN => F[OUT]): Unit = {
    routeCtx[F, IN, OUT](
      topic = topic,
      lockDuration = lockDuration,
      errorStrategy = errorStrategy,
      bpmnErrors = bpmnErrors,
      sendDiffOnly = sendDiffOnly,
      maxParallelism = maxParallelism,
      differentLoop = differentLoop
    )(ctx => handle(ctx.value))
  }

  def routeEither[
    F[_] : TopicBuilder: Sync,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      sendDiffOnly: Boolean = true,
                      maxParallelism: Int = 2,
                      differentLoop: Option[String] = None)
                     (handle: IN => F[Either[BpmnError, OUT]]): Unit = {
    routeCtxEither[F, IN, OUT](
      topic = topic,
      lockDuration = lockDuration,
      errorStrategy = errorStrategy,
      sendDiffOnly = sendDiffOnly,
      maxParallelism = maxParallelism,
      differentLoop = differentLoop
    )(ctx => handle(ctx.value))
  }

  def routeCtxEither[
    F[_] : TopicBuilder,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      sendDiffOnly: Boolean = true,
                      maxParallelism: Int = 2,
                      differentLoop: Option[String] = None)
                     (handle: CamundaContext[F, IN] => F[Either[BpmnError, OUT]]): Unit = {


    val subscription = new CamundaSubscriptionF[F, IN, OUT](
      processDefKey = TopicBuilder[F].processDefKey,
      topic = topic,
      lockDuration = lockDuration,
      errorStrategy = errorStrategy,
      sendDiffOnly = sendDiffOnly,
      maxParallelism = maxParallelism,
      differentLoop = differentLoop
    )(handle)
    TopicBuilder[F] += subscription
  }

  def routeCtx[
    F[_] : TopicBuilder: Sync,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      bpmnErrors: Boolean = false,
                      sendDiffOnly: Boolean = true,
                      maxParallelism: Int = 2,
                      differentLoop: Option[String] = None)
                     (handle: CamundaContext[F, IN] => F[OUT]): Unit = {
    routeCtxEither[F, IN, OUT](topic, lockDuration, errorStrategy, sendDiffOnly, maxParallelism, differentLoop) {
      ctx =>
        handle(ctx)
          .map(Either.right[BpmnError, OUT](_))
          .handleErrorWith(err =>
            if (bpmnErrors) Sync[F].delay(Left(BpmnError(err)))
            else Sync[F].raiseError(err)
          )
    }
  }


  def routeWithLockUpdate[F[_], IN: RootDecoder, OUT: RootEncoder](topic: String,
                                                                   lockDuration: FiniteDuration = 20.seconds,
                                                                   timeStep: FiniteDuration = 13.seconds,
                                                                   errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                                                                   bpmnErrors: Boolean = false,
                                                                   sendDiffOnly: Boolean = true,
                                                                   maxParallelism: Int = 100,
                                                                   differentLoop: Option[String] = None)
                                                                  (handle: IN => F[OUT])
                                                                  (implicit
                                                                   concurrent: Concurrent[F],
                                                                   builder: TopicBuilder[F],
                                                                   timer: Timer[F]): Unit = {
    import fs2.Stream
    routeCtx[F, IN, OUT](topic, lockDuration, errorStrategy, bpmnErrors, sendDiffOnly, maxParallelism, differentLoop) { ctx =>
      val handling = Stream.eval[F, OUT](handle(ctx.value))
      val prolongating = Stream.awakeEvery[F](timeStep)
        .evalMap(_ => ctx.service.extendLock(lockDuration)) // продлевается до времени = текущее время + lockDuration
        .filter(_ => false)
        .map(_.asInstanceOf[OUT])

      handling.mergeHaltBoth(prolongating)
        .compile
        .lastOrError
    }
  }


}

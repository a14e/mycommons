package a14e.commons.camunda.subscription

import java.time.{Duration, OffsetDateTime}
import java.util.UUID

import a14e.commons.context.{Contextual, LazyContextLogging}
import a14e.commons.camunda.client.CamundaClient
import a14e.commons.camunda.client.CamundaProtocol.{CompleteTaskRequest, ExtendLockRequest, ExternalTask, FetchAndLockRequest, FetchAndLockRequestTopic, TaskBpmnErrorRequest, TaskFailureRequest}
import a14e.commons.camunda.configuration.{CamundaConfigs, LoopSettings}
import cats.{Parallel, Traverse}
import cats.effect.{Concurrent, Sync, Timer}
import fs2.{Pipe, Stream}
import io.circe.Json

import scala.util.control.NonFatal

trait SubscriptionManager[F[_]] {

  def run(subscriptions: Seq[Subscription[F]]): F[Unit]

  def addFilter(filter: Filter[F]): SubscriptionManager[F]

  def withFilters(filters: Seq[Filter[F]]): SubscriptionManager[F]
}


// TODO убрать Contextual
class SubscriptionManagerImpl[F[_] : Concurrent : Contextual : Timer: Parallel](settings: CamundaConfigs,
                                                                                client: CamundaClient[F],
                                                                                filters: Seq[Filter[F]])
  extends SubscriptionManager[F]
    with LazyContextLogging {
  self =>

  import cats.implicits._
  import cats.effect.implicits._
  import a14e.commons.time.TimeImplicits._

  def withFilters(filters: Seq[Filter[F]]): SubscriptionManager[F] =
    new SubscriptionManagerImpl[F](settings, client, filters)

  def addFilter(filter: Filter[F]): SubscriptionManager[F] = withFilters(self.filters :+ filter)


  override def run(subscriptions: Seq[Subscription[F]]): F[Unit] = Sync[F].suspend {

    val workerId = "external-worker-" + UUID.randomUUID().toString

    val differentLoops = subscriptions.filter(_.differentLoop.isDefined)
      .flatMap { subscription =>
        subscription.differentLoop.map(loop => runSingleLoop(workerId, loop, Seq(subscription)))
      }

    val mainLoop = {
      val mainLoopSubscribtions = subscriptions.filter(_.differentLoop.isEmpty)
      runSingleLoop(workerId, settings.defaultLoop, mainLoopSubscribtions)
    }

    (mainLoop :: differentLoops.toList)
      .parSequence
      .void
  }


  private def runSingleLoop(workerId: String,
                            loop: LoopSettings,
                            subscriptions: Seq[Subscription[F]]): F[Unit] = {
    // в первых нет никаких processDefinitionKey, во вторых есть
    // но обработать может только один хендлер и мы выбираем более специфичный
    val noProcessSubs = subscriptions.filter(_.processDefinitionKeys.isEmpty).map(x => x.topic -> x).toMap
    val processSubs = (for {
      s <- subscriptions
      process <- s.processDefinitionKeys
      key = s.topic -> process
    } yield key -> s).toMap

    def findSubscription(topic: String,
                         processDefinitionKey: String): Option[Subscription[F]] = {
      val key = topic -> processDefinitionKey
      processSubs.get(key).orElse {
        noProcessSubs.get(topic)
      }
    }

    Stream.evalSeq {
      val request = buildFetchAndLockRequest(workerId, loop, subscriptions)
      client.fetchAndLock(request)
    }.through(logErrorAndRepeat("Task fetching error ", loop))
      .mapAsyncUnordered(loop.parallelLevel) { task =>
        val topic = task.topicName
        val processDefinitionKey = task.processDefinitionKey
        val foundTask = findSubscription(topic = topic, processDefinitionKey = processDefinitionKey)
        foundTask match {
          case None =>
            logger[F].warn(s"No subscription found for topic $topic, process $processDefinitionKey")
          case Some(_) if
          task.lockExpirationTime.plus(loop.lockSafetyTimeout).isBefore(OffsetDateTime.now()) =>
            logger[F].warn(s"Task lock time has been expired")
          case Some(subscription) =>
            handleTask(workerId, loop, subscription)(task)
        }
      }.through(logErrorAndRepeat("Camunda handling process failed with error", loop))
      .compile
      .drain
  }


  private def logErrorAndRepeat[T](text: String, loop: LoopSettings): Pipe[F, T, T] = {
    _.handleErrorWith {
      case NonFatal(err) =>
        Stream.eval {
          logger[F].error(text, err) *>
            Timer[F].sleep(loop.errorRestartTimeout)
        }.flatMap(_ => Stream.empty)
    }.repeat
  }


  @transient
  private lazy val mergedFilters = filters.fold(Filter.pure[F])((l, r) => l.merge(r))


  private def handleTask(workerId: String,
                         loop: LoopSettings,
                         subscription: Subscription[F])(task: ExternalTask[Json]): F[Unit] = {

    // todo название операции
    def handleClientError(err: Throwable): F[Unit] = {
      logger[F].error(s"Client call error for topic ${task.topicName}", err) *>
        Timer[F].sleep(loop.errorRestartTimeout) // мы должны замедлиться в случае, если завершение тасок не работает
    }

    def handleError(err: Throwable): F[Unit] = {
      val maxRetries = subscription.failureHandlingStrategy.maxRetries
      val retriesLeft = task.retries.getOrElse(maxRetries) - 1
      val nextStep = subscription.failureHandlingStrategy.nextTimeout(maxRetries - retriesLeft).getOrElse(0.millis)
      val request = TaskFailureRequest(
        workerId = workerId,
        errorMessage = err.getMessage,
        errorDetails = err.getStackTrace.mkString("|"),
        retries = retriesLeft,
        retryTimeout = nextStep.toMillis
      )
      client.failureTask(task.id, request)
        .handleErrorWith(handleClientError)
    }

    def convertToCamundaJson(initVariables: Json,
                             resultVariables: Json): Json = {
      val diff = if (subscription.sendDiffOnly) diffJson(initVariables, resultVariables) else resultVariables
      convertJsonToCamundaVariables(diff)
    }

    val taskHandling = Sync[F].delay(parseCamundaVaribales(task.variables))
      .flatMap { initVariables =>

        val newTask = task.copy(variables = initVariables)
        mergedFilters.filter(newTask, subscription.handling, subscription)
          .flatMap {
            case Left(bpmnError: BpmnError[Json]) =>
              val convertedVars = convertToCamundaJson(initVariables, bpmnError.variables)
              val request = TaskBpmnErrorRequest(workerId, bpmnError.errorCode, bpmnError.errorMessage, convertedVars)
              (logger[F].error(s"Task handling completed with bpmn error") *>
                client.bpmnError(task.id, request)
                  .handleErrorWith(handleClientError))
                .uncancelable
            case Right(resultVariables) =>
              val convertedVars = convertToCamundaJson(initVariables, resultVariables)
              val request = CompleteTaskRequest(workerId, convertedVars)
              (logger[F].info(s"Task handling completed with success") *>
                client.completeTask(task.id, request)
                  .handleErrorWith(handleClientError))
                .uncancelable
          }
      }.handleErrorWith {
      case NonFatal(err) =>
        (logger[F].error(s"Task handling error for topic ${task.topicName}", err) *>
          handleError(err))
          .uncancelable
    }

    val lockExtendingStream = {
      if (subscription.extendLock) Stream.never
      else Stream.awakeDelay[F](loop.extendLockTimeout)
        .evalMap { _ =>
          val request = ExtendLockRequest(workerId, loop.lockTimeout.toMillis)
          client.extendLock(task.id, request) // продлевается до времени = текущее время + lockDuration
            .handleErrorWith(handleClientError)
        }
        .filter(_ => false)
        .covaryAll[F, Unit]
    }

    // Защита от зависших тасок, чтобы дать другим задачам возможность выполнится
    // TODO в отдельную переменную вместо extendLock
    val haltingStream = {
      if (!subscription.extendLock) Stream.never
      else {
        Stream.eval {
          Timer[F].sleep(loop.lockTimeout).flatMap { _ =>
            logger[F].error(s"Stopping handling of topic ${task.topicName} by timeout")
          }
        }
      }
    }

    Stream.eval(taskHandling)
      .mergeHaltBoth(lockExtendingStream)
      .mergeHaltBoth(haltingStream)
      .compile
      .drain
  }

  // тут мы узнаем diff между переменными, чтобы отправлять не все сразу
  private def diffJson(oldJson: Json,
                       newJson: Json): Json = {
    (oldJson.asObject, newJson.asObject) match {
      case (Some(oldO), Some(newO)) =>
        val diff = newO.filter { case (k, v) => !oldO.apply(k).contains(v) }
        Json.fromJsonObject(diff)
      case _ => throw new RuntimeException("Variables should be a valid json object " + newJson.spaces4)
    }
  }

  // тут мы упаковываем переменные в формат камунды
  // по факту мы просто подклаываем значения в value, а обьекты кладем с типом JSON
  // более сложные форматы поддерживать нецелесообразно
  private def convertJsonToCamundaVariables(json: Json): Json = {
    json.asObject match {
      case None => throw new RuntimeException("Variables should be valid json object " + json.spaces4)
      case Some(obj) =>
        val newObj = obj.mapValues {
          case value if value.isObject => Json.obj(
            "value" -> Json.fromString(value.noSpaces),
            "type" -> Json.fromString("json")
          )
          case value => Json.obj("value" -> value)
        }
        Json.fromJsonObject(newObj)
    }
  }

  private def parseCamundaVaribales(json: Json): Json = {
    val jsonString = Json.fromString("Json")
    json.asObject match {
      case None => throw new RuntimeException("Variables should be valid json object " + json.spaces4)
      case Some(obj) =>
        val newObj = obj.mapValues { value =>
          (for {
            obj <- value.asObject
            value <- obj.apply("value")
            isJson = obj.apply("type").contains(jsonString)
            result <- {
              if (isJson) value.asString.map(str => io.circe.parser.parse(str).toTry.get)
              else Some(value)
            }
          } yield result).getOrElse(value)
        }
        Json.fromJsonObject(newObj)
    }
  }


  private def buildFetchAndLockRequest(workerId: String,
                                       loop: LoopSettings,
                                       subscriptions: Seq[Subscription[F]]): FetchAndLockRequest = {
    FetchAndLockRequest(
      workerId = workerId,
      maxTasks = loop.fetchSize,
      usePriority = loop.usePriority,
      asyncResponseTimeout = loop.asyncResponseTimeout.toMillis,
      topics =
        subscriptions.map { s =>
          FetchAndLockRequestTopic(
            topicName = s.topic,
            lockDuration = loop.lockTimeout.toMillis,
            processDefinitionKeyIn = s.processDefinitionKeys
          )
        }
    )
  }

}

package a14e.commons.camundadsl

import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.mdc.ContextEffect
import cats.data.EitherT
import cats.effect.{ContextShift, Effect, IO, Sync}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.camunda.bpm.client.ExternalTaskClient
import org.camunda.bpm.client.task.{ExternalTask, ExternalTaskService}
import org.camunda.bpm.client.topic.TopicSubscription

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

object Types {

  case class CamundaContext[F[_], IN](service: CamundaTaskService[F],
                                      task: ExternalTask,
                                      value: IN)


}

trait CamundaSubscription[F[_]] {
  def run(client: ExternalTaskClient): F[TopicSubscription]
}

class CamundaSubscriptionF[
  F[_] : ContextShift : Effect,
  IN: RootDecoder,
  OUT: RootEncoder](processDefKey: String,
                    topic: String,
                    lockDuration: FiniteDuration,
                    errorStrategy: ErrorStrategy)
                   (handler: CamundaContext[F, IN] => EitherT[F, BpmnError, OUT])
  extends LazyLogging
    with CamundaSubscription[F] {

  override def run(client: ExternalTaskClient): F[TopicSubscription] = Sync[F].delay {
    client.subscribe(topic)
      .processDefinitionKey(processDefKey)
      .lockDuration(lockDuration.toMillis)
      .handler { (task: ExternalTask, service: ExternalTaskService) =>
        // тут шифт, чтобы сразу перескочить на рабочие потоки и не грузить эвент луп камунды (в клиенте всего 1 поток)
        Effect[F].runAsync(ContextShift[F].shift *> ContextEffect.addContext[F]() *> buildHandlingF(task, service)) {
          case Left(err) =>
            IO.delay(logger.error(s"Handling of topic $topic failed with error", err))
          case Right(_) =>
            IO.delay(logger.info(s"Handling of topic $topic completed with success"))
        }.toIO
          .unsafeRunAsyncAndForget()
      }.open()
  }

  private def buildHandlingF(task: ExternalTask,
                             javaService: ExternalTaskService): F[_] = Sync[F].defer {
    logger.info(s"Received task for topic $topic. businessKey = ${task.getBusinessKey}. ${task.getAllVariablesTyped}")
    // TODO handling of decoding errors
    implicit val wrapperService: CamundaTaskService[F] = new CamundaTaskService[F](task, javaService)
    val decodedF: F[IN] = Sync[F].fromTry(RootDecoder[IN].decode(task))
      .handleErrorWith(err => ErrorHandling.handleError(err, ErrorStrategy.shotRetries))

    (for {
      decoded <- decodedF
      context = CamundaContext(wrapperService, task, decoded)
      result <-  handler(context).value
      _ <- result match {
        case Left(BpmnError(err)) => ErrorHandling.handleBpmnErr(err)
        case Right(out) => wrapperService.complete(out)
      }
    } yield ()).handleErrorWith(err => ErrorHandling.handleError(err, errorStrategy))
  }
}

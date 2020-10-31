package a14e.commons.camundadsl

import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.mdc.{ContextEffect, MdcEffect}
import cats.{MonadError, Traverse, ~>}
import cats.data.EitherT
import cats.effect.{ContextShift, Effect, ExitCase, IO, Sync}
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
                                      businessKey: String,
                                      value: IN) {
    def to[B[_] : Sync : ContextShift]: CamundaContext[B, IN] = copy(service = service.to[B])
  }


}

trait CamundaSubscription[F[_]] {
  val topic: String

  def run(client: ExternalTaskClient,
          runCallBack: F[_] => Unit)
         (implicit
          shift: ContextShift[F],
          sync: Sync[F]): F[TopicSubscription]

  def to[B[_]](to: F ~> B)
              (implicit
               sync: Sync[F],
               shift: ContextShift[F]): CamundaSubscription[B]
}

class CamundaSubscriptionF[
  F[_],
  IN: RootDecoder,
  OUT: RootEncoder](processDefKey: String,
                    override val topic: String,
                    lockDuration: FiniteDuration,
                    errorStrategy: ErrorStrategy,
                    sendDiffOnly: Boolean = true)
                   (handler: CamundaContext[F, IN] => F[Either[BpmnError, OUT]])
  extends LazyLogging
    with CamundaSubscription[F] {
  self =>

  override def to[B[_]](to: F ~> B)
                       (implicit
                        sync: Sync[F],
                        shift: ContextShift[F]): CamundaSubscriptionF[B, IN, OUT] = {
    new CamundaSubscriptionF[B, IN, OUT](
      self.processDefKey,
      self.topic,
      self.lockDuration,
      self.errorStrategy,
      self.sendDiffOnly)({ ctx: CamundaContext[B, IN] =>
      val newCtx = ctx.to[F]
      to(handler(newCtx))
    })
  }

  override def run(client: ExternalTaskClient,
                   runCallBack: F[_] => Unit)
                  (implicit
                   shift: ContextShift[F],
                   effect: Sync[F]): F[TopicSubscription] = Sync[F].delay {
    client.subscribe(topic)
      .processDefinitionKey(processDefKey)
      .lockDuration(lockDuration.toMillis)
      .handler { (task: ExternalTask, service: ExternalTaskService) =>
        // тут шифт, чтобы сразу перескочить на рабочие потоки и не грузить эвент луп камунды (в клиенте всего 1 поток)
        runCallBack(buildHandlingF(task, service))
      }.open()
  }

  private def buildHandlingF(task: ExternalTask,
                             javaService: ExternalTaskService)
                            (implicit
                             shift: ContextShift[F],
                             sync: Sync[F]): F[_] = Sync[F].defer {
    logger.info(s"Received task for topic $topic. businessKey = ${task.getBusinessKey}. ${task.getAllVariablesTyped}")
    implicit val wrapperService: CamundaTaskService[F] = new CamundaTaskService[F](task, javaService)
    val decodedF: F[IN] = Sync[F].fromTry(RootDecoder[IN].decode(task))
      .handleErrorWith(err => ErrorHandling.handleError(err, ErrorStrategy.failAndStop))

    (for {
      decoded <- decodedF
      context = CamundaContext(wrapperService, task, task.getBusinessKey, decoded)
      result <- handler(context)
      _ <- result match {
        case Left(BpmnError(err)) => ErrorHandling.handleBpmnErr(err)
        case Right(out) if !sendDiffOnly => wrapperService.complete(out)
        case Right(out) =>
          // диф помогает ускорить обработку за счет экономии на io и записи лишних значений в базу =)
          // плюс убирает лишние изменения из истории
          val encoded = RootEncoder[OUT].encodeDiffOnly(out, task.getAllVariablesTyped)
          wrapperService.complete(encoded)
      }
    } yield ()).handleErrorWith(err => ErrorHandling.handleError(err, errorStrategy))
  }
}

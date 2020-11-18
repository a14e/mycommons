package a14e.commons.camundadsl

import java.util.concurrent.atomic.AtomicLong

import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.context.{Contextual, LazyContextLogging}
import cats.{MonadError, Traverse, ~>}
import cats.data.EitherT
import cats.effect.{ConcurrentEffect, ContextShift, Effect, ExitCase, IO, Sync}
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
                                      value: IN)

}

trait CamundaSubscription[F[_]] {
  def topic: String

  def maxParallelism: Int


  def differentLoop: Option[String]

  def run(client: ExternalTaskClient)
         (implicit
          shift: ContextShift[F],
          sync: ConcurrentEffect[F],
          cont: Contextual[F]): F[TopicSubscription]


}

class CamundaSubscriptionF[
  F[_],
  IN: RootDecoder,
  OUT: RootEncoder](processDefKey: String,
                    override val topic: String,
                    lockDuration: FiniteDuration,
                    errorStrategy: ErrorStrategy,
                    sendDiffOnly: Boolean,
                    override val maxParallelism: Int,
                    override val differentLoop: Option[String] = None)
                   (handler: CamundaContext[F, IN] => F[Either[BpmnError, OUT]])
  extends LazyContextLogging
    with CamundaSubscription[F] {
  self =>

  import cats.effect.implicits._
  import cats.implicits._


  override def run(client: ExternalTaskClient)
                  (implicit
                   shift: ContextShift[F],
                   effect: ConcurrentEffect[F],
                   cont: Contextual[F]): F[TopicSubscription] = Sync[F].delay {
    client.subscribe(topic)
      .processDefinitionKey(processDefKey)
      .lockDuration(lockDuration.toMillis)
      .handler { (task: ExternalTask, service: ExternalTaskService) =>
        val parallelism = parallelismCounter.incrementAndGet()
        // тут шифт, чтобы сразу перескочить на рабочие потоки и не грузить эвент луп камунды (в клиенте всего 1 поток)
        val io = (ContextShift[F].shift *> buildHandlingF(task, service))
          .attempt
          .flatMap {
            case Left(err) =>
              logger[F].error(s"Handling of topic $topic failed with error", err)
            case Right(_) =>
              logger[F].info(s"Handling of topic $topic completed with success")
          }
          .toIO
          .guarantee(IO.delay(parallelismCounter.decrementAndGet()))

        // simple backpressure =)
        if (parallelism < maxParallelism) io.unsafeRunAsyncAndForget()
        else io.unsafeRunSync()
      }.open()
  }

  private val parallelismCounter = new AtomicLong(0L)

  private def buildHandlingF(task: ExternalTask,
                             javaService: ExternalTaskService)
                            (implicit
                             shift: ContextShift[F],
                             sync: Sync[F],
                             cont: Contextual[F]): F[_] = Sync[F].defer {
    implicit val wrapperService: CamundaTaskService[F] = new CamundaTaskService[F](task, javaService)
    val decodedF: F[IN] = Sync[F].fromTry(RootDecoder[IN].decode(task))
      .handleErrorWith(err => ErrorHandling.handleError(err, ErrorStrategy.failAndStop))

    logger[F].info(s"Received task for topic $topic. businessKey = ${task.getBusinessKey}. ${task.getAllVariablesTyped}") *>
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

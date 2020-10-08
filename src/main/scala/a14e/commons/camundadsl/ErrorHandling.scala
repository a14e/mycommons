package a14e.commons.camundadsl

import java.time.Duration

import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.time.RichDuration
import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._

import scala.language.higherKinds


object ErrorHandling {


  def handleError[F[_] : Sync : ContextShift](context: CamundaContext[F, _],
                                              err: Throwable,
                                              strategy: ErrorStrategy): F[Unit] = {
    strategy match {
      case ErrorStrategy.BpmnError => bpmnErr(context, err)
      case f@ErrorStrategy.Failure(_, _) => failure(context, err, f)
    }
  }

  private def failure[F[_] : Sync : ContextShift](context: CamundaContext[F, _],
                                                  err: Throwable,
                                                  failStrategy: ErrorStrategy.Failure): F[Unit] = {
    val retriesLeft = Option(context.task.getRetries).map(_.intValue()).getOrElse(failStrategy.retries)
    val nextStep = failStrategy.retryStep.nextStep(failStrategy.retries - retriesLeft)
    context.service
      .handleFailure(err.getMessage, err.getStackTrace.mkString("|"), retriesLeft, nextStep.toMillis)
      .flatMap(_ => throw err)
  }

  private def bpmnErr[F[_] : Sync : ContextShift](context: CamundaContext[F, _],
                                                  err: Throwable): F[Unit] = {
    import Encodings.auto._
    case class LastError(lastError: String)
    implicit val encoder: RootEncoder[LastError] = Encodings.semiauto.derivedEncoder[LastError]()
    context.service
      .handleBpmnError(err.getMessage, err.getStackTrace.mkString("|"), LastError(err.getMessage))
      .flatMap(_ => throw err)
  }
}

sealed trait ErrorStrategy

object ErrorStrategy {

  import a14e.commons.time.TimeImplicits._

  // TODO нормальная экспоненциальная обработка ошибок
  val failAndStop: ErrorStrategy = Failure(0, ConstStep(0.seconds))
  val shotRetries: ErrorStrategy = Failure(3, ConstStep(10.seconds))
  val simpleRetries: ErrorStrategy = Failure(10, ConstStep(10.seconds))
  // консервативная стратегия с повторами через 1 секунду и экспоненциальным ростом
  val simpleExpRetries: ErrorStrategy = Failure(10, ExpStep(1.seconds, 4, 20.minutes))
  val bpmnError: ErrorStrategy = BpmnError

  case object BpmnError extends ErrorStrategy

  case class Failure(retries: Int, retryStep: RetryStep) extends ErrorStrategy {
    assert(retries >= 0)
  }

  trait RetryStep {
    def nextStep(retry: Int): Duration
  }

  case class ConstStep(step: Duration) extends RetryStep {
    assert(step.toMillis >= 0)

    override def nextStep(retry: Int): Duration = step
  }

  case class ExpStep(init: Duration,
                     ratio: Double,
                     max: Duration) extends RetryStep {
    assert(ratio >= 1.0)
    assert(max.toMillis > 0)
    assert(init.toMillis > 0)

    override def nextStep(retry: Int): Duration = {
      val multiplier: Long = math.pow(ratio, retry).toLong
      if (multiplier < 0L) max
      else multiplier * init
    }
  }

}
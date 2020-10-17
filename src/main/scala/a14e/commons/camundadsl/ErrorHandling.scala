package a14e.commons.camundadsl

import java.time.Duration

import a14e.commons.camundadsl.ErrorStrategy.RetryStep
import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.time.RichDuration
import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._

import scala.language.higherKinds


object ErrorHandling {

  def handleError[F[_] : Sync : ContextShift : CamundaTaskService, T](err: Throwable,
                                                                      strategy: ErrorStrategy): F[T] = {
    val service = CamundaTaskService[F]
    val task = service.task
    val retriesLeft = Option(task.getRetries).map(_.intValue()).getOrElse(strategy.retries)
    val nextStep = strategy.retryStep.nextStep(strategy.retries - retriesLeft)
    service
      .handleFailure(err.getMessage, err.getStackTrace.mkString("|"), retriesLeft, nextStep.toMillis)
      .flatMap(_ => throw err)
  }


  def handleBpmnErr[F[_] : Sync : ContextShift : CamundaTaskService, T](err: Throwable): F[T] = {
    import Encodings.auto._
    case class LastError(lastError: String)
    implicit val encoder: RootEncoder[LastError] = Encodings.semiauto.derivedEncoder[LastError]()
    val service = CamundaTaskService[F]
    service
      .handleBpmnError(err.getMessage, err.getStackTrace.mkString("|"), LastError(err.getMessage))
      .flatMap(_ => Sync[F].raiseError(err))
  }
}


case class BpmnError(err: Throwable)

case class ErrorStrategy(retries: Int,
                         retryStep: RetryStep) {
  assert(retries >= 0)
}

object ErrorStrategy {

  import a14e.commons.time.TimeImplicits._

  // TODO нормальная экспоненциальная обработка ошибок
  val failAndStop: ErrorStrategy = ErrorStrategy(0, ConstStep(0.seconds))
  val shotRetries: ErrorStrategy = ErrorStrategy(3, ConstStep(10.seconds))
  val simpleRetries: ErrorStrategy = ErrorStrategy(10, ConstStep(10.seconds))
  // консервативная стратегия с повторами через 1 секунду и экспоненциальным ростом
  val simpleExpRetries: ErrorStrategy = ErrorStrategy(10, ExpStep(1.seconds, 4, 30.minutes))

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
      val nextValue = multiplier * init

      if (nextValue.toMillis <= 0L) max
      else if (nextValue > max) max
      else nextValue
    }
  }

}
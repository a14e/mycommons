package a14e.commons.camundadsl

import a14e.commons.camundadsl.Types.CamundaContext
import cats.effect.{ContextShift, Sync}
import cats.syntax.flatMap._
import scala.concurrent.duration._
import scala.language.higherKinds


object ErrorHandling {


  def handleError[F[_] : Sync : ContextShift](context: CamundaContext[F, _],
                                              err: Throwable,
                                              strategy: ErrorStrategy): F[Unit] = {
    strategy match {
      case ErrorStrategy.BpmnError => bpmnErr(context, err)
      case ErrorStrategy.Failure(retries, timeout) => failure(context, err, retries, timeout)
    }
  }

  private def failure[F[_] : Sync : ContextShift](context: CamundaContext[F, _],
                                                  err: Throwable,
                                                  retries: Int,
                                                  timeout: FiniteDuration): F[Unit] = {
    val retriesLeft = Option(context.task.getRetries).map(_.intValue()).getOrElse(retries)
    context.service
      .handleFailure(err.getMessage, err.getStackTrace.mkString("|"), retriesLeft, timeout.toMillis)
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
  // TODO нормальная экспоненциальная обработка ошибок
  val failAndStop: ErrorStrategy = Failure(0, 0.seconds)
  val shotRetries: ErrorStrategy = Failure(3, 10.seconds)
  val simpleRetries: ErrorStrategy = Failure(10, 10.seconds)
  val bpmnError: ErrorStrategy = BpmnError

  def customRetries(retries: Int,
                    retryTimeout: FiniteDuration): ErrorStrategy = Failure(retries, retryTimeout)

  case object BpmnError extends ErrorStrategy

  case class Failure(retries: Int, retryTimeout: FiniteDuration) extends ErrorStrategy {
    assert(retries >= 0)
  }

}
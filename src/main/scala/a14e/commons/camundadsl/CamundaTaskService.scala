package a14e.commons.camundadsl

import java.util.concurrent.Executors

import a14e.commons.context.{Contextual, LazyContextLogging}
import cats.effect.{ContextShift, Sync}
import com.typesafe.scalalogging.LazyLogging
import org.camunda.bpm.client.task.{ExternalTask, ExternalTaskService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds


object CamundaTaskService {
  def apply[F[_]: CamundaTaskService]: CamundaTaskService[F] = implicitly[CamundaTaskService[F]]
}

object CamundaPull {
  // тут ок cached, так как основной источник событий -- сама камунда и если упадет камунда, то у нас не будет
  // источников для некотролируемого роста числа потоков
  val blockingCamundaPull = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}

class CamundaTaskService[F[_] : Sync : ContextShift : Contextual](val task: ExternalTask,
                                                                  val underlying: ExternalTaskService,
                                                                  blockingContext: ExecutionContext = CamundaPull.blockingCamundaPull) extends LazyContextLogging {

  import cats.implicits._

  def to[B[_] : Sync : ContextShift : Contextual] = new CamundaTaskService[B](task, underlying, blockingContext)


  def complete(): F[Unit] = {

    logger[F].info("Completing task message") *>
      blocked {
        underlying.complete(task)
      }
  }


  def complete[T: RootEncoder](variables: T): F[Unit] = {
    val encoded = RootEncoder[T].encode(variables)
    logger[F].info(s"Sending message $encoded") *>
      blocked {
        underlying.complete(
          task,
          encoded
        )
      }
  }

  def complete[T: RootEncoder, B: RootEncoder](variables: T,
                                               localVariables: B): F[Unit] = {
    val encodedVars = RootEncoder[T].encode(variables)
    val encodedLocalVars = RootEncoder[B].encode(localVariables)
    logger[F].info(s"Sending message $encodedVars.  $encodedLocalVars") *>
      blocked {
        underlying.complete(
          task,
          encodedVars,
          encodedLocalVars
        )
      }
  }

  def handleFailure(errorMessage: String,
                    errorDetails: String,
                    retries: Int,
                    retryTimeout: Long): F[Unit] = {
    logger[F].error(s"Sending error $errorMessage") *>
      blocked {
        underlying.handleFailure(
          task,
          errorMessage,
          errorDetails,
          retries, // TODO продумать стратегию ретраев
          retryTimeout
        )
      }
  }

  def handleBpmnError(errorCode: String): F[Unit] = {
    logger[F].error(s"Sending bpmnError code $errorCode") *>
      blocked {
        underlying.handleBpmnError(task, errorCode)
      }
  }


  def handleBpmnError(errorCode: String,
                      errorMessage: String): F[Unit] = {
    logger[F].error(s"Sending bpmnError code $errorCode") *>
      blocked {
        underlying.handleBpmnError(task, errorCode, errorMessage)
      }
  }


  def handleBpmnError[T: RootEncoder](errorCode: String,
                                      errorMessage: String,
                                      variables: T): F[Unit] = {
    val encoded = RootEncoder[T].encode(variables)
    logger[F].error(s"Sending bpmnError  code $errorCode. error variables $encoded") *>
      blocked {
        underlying.handleBpmnError(
          task,
          errorCode,
          errorMessage,
          RootEncoder[T].encode(variables)
        )
      }
  }

  def extendLock(newDuration: FiniteDuration): F[Unit] = {
    logger[F].info(s"Extending lock to $newDuration") *>
      blocked {
        underlying.extendLock(task, newDuration.toMillis)
      }
  }

  private def blocked[T](f: => T): F[T] = {
    ContextShift[F].evalOn(this.blockingContext)(Sync[F].delay(f))
  }

}


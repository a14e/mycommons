package a14e.commons.camundadsl

import java.util.concurrent.Executors

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

class CamundaTaskService[F[_] : Sync : ContextShift](val task: ExternalTask,
                                                     val underlying: ExternalTaskService,
                                                     blockingContext: ExecutionContext = CamundaPull.blockingCamundaPull) extends LazyLogging {


  def complete(): F[Unit] = {
    blocked {
      logger.info("Completing task message")
      underlying.complete(task)
    }
  }


  def complete[T: RootEncoder](variables: T): F[Unit] = {
    blocked {
      val encoded = RootEncoder[T].encode(variables)
      logger.info(s"Sending message $encoded")
      underlying.complete(
        task,
        encoded
      )
    }
  }

  def complete[T: RootEncoder, B: RootEncoder](variables: T,
                                               localVariables: B): F[Unit] = {
    blocked {
      val encodedVars = RootEncoder[T].encode(variables)
      val encodedLocalVars = RootEncoder[B].encode(localVariables)
      logger.info(s"Sending message $encodedVars.  $encodedLocalVars")
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
    blocked {
      logger.error(s"Sending error $errorMessage")
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
    blocked {
      logger.error(s"Sending bpmnError code $errorCode")
      underlying.handleBpmnError(task, errorCode)
    }
  }


  def handleBpmnError(errorCode: String,
                      errorMessage: String): F[Unit] = {
    blocked {
      logger.error(s"Sending bpmnError code $errorCode")
      underlying.handleBpmnError(task, errorCode, errorMessage)
    }
  }


  def handleBpmnError[T: RootEncoder](errorCode: String,
                                      errorMessage: String,
                                      variables: T): F[Unit] = {
    blocked {
      val encoded = RootEncoder[T].encode(variables)
      logger.error(s"Sending bpmnError  code $errorCode. error variables $encoded")
      underlying.handleBpmnError(
        task,
        errorCode,
        errorMessage,
        RootEncoder[T].encode(variables)
      )
    }
  }

  def extendLock(newDuration: FiniteDuration): F[Unit] = {
    blocked {
      logger.info(s"Extending lock to $newDuration")
      underlying.extendLock(task, newDuration.toMillis)
    }
  }

  private def blocked[T](f: => T): F[T] = {
    ContextShift[F].evalOn(this.blockingContext)(Sync[F].delay(f))
  }

}


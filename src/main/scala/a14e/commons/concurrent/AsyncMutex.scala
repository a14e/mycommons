package a14e.commons.concurrent

import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, Future}
import FutureImplicits._
import scala.concurrent.duration._

trait AsyncMutex {

  def apply[T](block: => T)
              (implicit timeout: FiniteDuration = 100.seconds): Future[T]

  def async[T](block: => Future[T])
              (implicit timeout: FiniteDuration = 100.seconds): Future[T]
}

object AsyncMutex {
  def apply()(implicit
              executionContext: ExecutionContext,
              actorSystem: ActorSystem): AsyncMutex = new AsyncMutexImpl()
}

class AsyncMutexImpl(implicit
                     executionContext: ExecutionContext,
                     actorSystem: ActorSystem) extends AsyncMutex {

  override def apply[T](block: => T)
                       (implicit timeout: FiniteDuration = 100.seconds): Future[T] = {
    async(Future.successful(block))(timeout)
  }

  override def async[T](block: => Future[T])
                       (implicit timeout: FiniteDuration = 100.seconds): Future[T] = lock.synchronized {
    val res = lastObject.flatMap(_ => block)
      .replaceAfter(timeout)(throw new RuntimeException("Execution timeout"))
    lastObject = res.extractTry
    res
  }

  private var lastObject: Future[Any] = Future.unit
  private val lock = new Object
}
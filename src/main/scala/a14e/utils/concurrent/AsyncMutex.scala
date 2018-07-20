package a14e.utils.concurrent

import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, Future}
import FutureImplicits._
import scala.concurrent.duration._

trait AsyncMutex {

  def apply[T](block: => T): Future[T]

  def async[T](block: => Future[T]): Future[T]
}


class AsyncMutexImpl(implicit
                     executionContext: ExecutionContext,
                     actorSystem: ActorSystem) extends AsyncMutex {

  override def apply[T](block: => T): Future[T] = lock.synchronized {
    val res = lastObject.map(_ => block)
    lastObject = res.extractTry.replaceAfter(100.seconds)(throw new RuntimeException("Execution timeout"))
    res
  }

  override def async[T](block: => Future[T]): Future[T] = lock.synchronized {
    val res = lastObject.flatMap(_ => block)
    lastObject = res.extractTry.replaceAfter(100.seconds)(throw new RuntimeException("Execution timeout"))
    res
  }

  private var lastObject: Future[Any] = Future.unit
  private val lock = new Object
}
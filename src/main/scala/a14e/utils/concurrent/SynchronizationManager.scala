package a14e.utils.concurrent

import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.{ReentrantLock, ReentrantReadWriteLock, StampedLock}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.mutable
import scala.util.Try
import FutureImplicits._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import SyncActor._

trait SynchronizationManager {
  def sync[T, KEY <: AnyRef](key: KEY)(block: => Future[T]): Future[T]
}

class ActorSynchronizationManagerImpl(actorSystem: ActorSystem)
                                     (implicit context: ExecutionContext) extends SynchronizationManager {

  override def sync[T, KEY <: AnyRef](key: KEY)(block: => Future[T]): Future[T] = {

    for {
      _ <- underlying ? Acquire(key)
      resTry <- handleErrors(block)
      _ = underlying ! Release(key)
      res <- Future.fromTry(resTry)
    } yield res
  }

  private def handleErrors[T](block: => Future[T]): Future[Try[T]] = {
    Future.handleWith {
      block.replaceAfter(timeout.duration)(throw new TimeoutException)
    }.extractTry
  }

  // TODO вынести в конфиги
  implicit private val timeout: Timeout = Timeout(2 minutes)
  implicit private val system: ActorSystem = actorSystem
  private lazy val underlying = actorSystem.actorOf(Props(new SyncActor))
}

object SyncActor {
  private[concurrent] case object Done
  private[concurrent] case class Acquire(key: AnyRef)
  private[concurrent] case class Release(key: AnyRef)
}

// TODO максимальный размер очереди
private class SyncActor extends Actor {
  override def receive: Receive = {
    case Acquire(key) => acquire(key)
    case Release(key) => release(key)
  }

  def acquire(key: AnyRef): Unit = {
    val queue = _callBacks.getOrElseUpdate(key, mutable.Queue[ActorRef]())
    if (queue.isEmpty) sender() ! Done
    queue += sender()
  }

  def release(key: AnyRef): Unit = {
    _callBacks.get(key) match {
      case None =>
      case Some(queue) =>
        queue.dequeue() // потому что текущий уже выполняется
        if (queue.isEmpty)
          _callBacks -= key
        else
          queue.head ! Done
    }
  }

  override def postStop(): Unit = _callBacks.values.foreach(_.foreach(_ ! Done))

  private val _callBacks = new mutable.HashMap[AnyRef, mutable.Queue[ActorRef]]
}

object FakeSynchronizationManager extends SynchronizationManager {
  override def sync[T, KEY <: AnyRef](key: KEY)(block: => Future[T]): Future[T] = block
}
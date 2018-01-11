package a14e.utils.concurrent

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import scala.collection.generic.CanBuildFrom
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try
import scala.util.control.NonFatal
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import akka.util.Timeout._
import akka.http.scaladsl.util.FastFuture._
import akka.http.scaladsl.util.FastFuture._
import akka.http.scaladsl.util.FastFuture
import FutureImplicits._
import akka.actor.{ActorSystem, Cancellable}

object FutureImplicits {
  lazy val sameThreadExecutionContext: ExecutionContext = new ExecutionContext {
    override def execute(runnable: Runnable): Unit = runnable.run()

    override def reportFailure(cause: Throwable): Unit = ()
  }

  implicit def Future2RichFuture[T](f: Future[T]): RichFuture[T] = new RichFuture[T](f)

  implicit def FutureObj2RichFuture(f: Future.type): RichFutureObj.type = RichFutureObj
}

import FutureImplicits._

class RichFuture[T](val future: Future[T]) extends AnyVal {
  def toUnit: Future[Unit] = future.fast.map(_ => ())(sameThreadExecutionContext)

  def toFast: Future[T] = FastFuture.successful(future).flatMap(identity)(sameThreadExecutionContext)

  def extractTry: Future[Try[T]] = future.transform(Try(_))(sameThreadExecutionContext)

  def wrapToOption: Future[Option[T]] =
    future
      .map(Some(_))(sameThreadExecutionContext)
      .recover { case _ => None }(sameThreadExecutionContext)

  def headOption[B](implicit conv: T <:< Seq[B]): Future[Option[B]] =
    future.map(_.headOption)(sameThreadExecutionContext)


  def replaceAfterWith[B >: T](timeout: FiniteDuration)
                              (anotherValue: => Future[B])
                              (implicit
                               context: ExecutionContext,
                               system: ActorSystem): Future[B] = {
    // TODO проброс контекста
    val resultPromise = Promise[B]()
    val isCompleted = new AtomicBoolean(false)
    val onTimeoutPromise = Promise[Unit]()
    val closable = system.scheduler.scheduleOnce(timeout)(onTimeoutPromise.success(()))

    def doOnPrimaryTask(value: Try[T]): Unit = {
      if (!isCompleted.getAndSet(true)) {
        closable.cancel()
        resultPromise.complete(value)
      }
    }

    def doOnSecondaryTask(): Unit = {
      if (!isCompleted.getAndSet(true)) {
        val result: Future[B] = Future.handleWith(anotherValue)
        result.onComplete(resultPromise.complete)
      }
    }

    onTimeoutPromise.future.foreach(_ => doOnSecondaryTask())
    future.onComplete(doOnPrimaryTask)

    resultPromise.future
  }

  def replaceAfter[B >: T](timeout: FiniteDuration)
                          (anotherValue: => B)
                          (implicit context: ExecutionContext,
                           system: ActorSystem): Future[B] = {
    replaceAfterWith[B](timeout)(Future.successful(anotherValue))
  }
}


object RichFutureObj {

  def serially[T, M[X] <: TraversableOnce[X], B](coll: M[T])
                                                (fun: T => Future[B])
                                                (implicit
                                                 context: ExecutionContext,
                                                 canBuildFrom: CanBuildFrom[M[T], B, M[B]]): Future[M[B]] = {
    val init = FastFuture.successful(canBuildFrom(coll))
    coll.foldLeft(init) { (pevFuture, current) =>
      for {
        builder <- pevFuture
        next <- fun(current)
        updatedBuilder = builder += next
      } yield updatedBuilder
    }.map(_.result())
  }

  def handleWith[T](block: => Future[T]): Future[T] = {
    try {
      block
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }

  def handle[T](block: => T): Future[T] = {
    try {
      Future.successful(block)
    } catch {
      case e: Throwable => Future.failed(e)
    }
  }
}


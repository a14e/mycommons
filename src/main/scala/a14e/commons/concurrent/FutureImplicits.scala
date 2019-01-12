package a14e.commons.concurrent

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
import com.typesafe.scalalogging.Logger


object FutureImplicits {
  implicit def Future2RichFuture[T](f: Future[T]): RichFuture[T] = new RichFuture[T](f)

  implicit def FutureObj2RichFuture(f: Future.type): RichFutureObj.type = RichFutureObj
}

class RichFuture[T](val future: Future[T]) extends AnyVal {
  def toUnit: Future[Unit] = future.fast.map(_ => ())(FutureUtils.sameThreadExecutionContext)

  def toFast: Future[T] = FastFuture.successful(future).flatMap(identity)(FutureUtils.sameThreadExecutionContext)

  def extractTry: Future[Try[T]] = future.transform(Try(_))(FutureUtils.sameThreadExecutionContext)

  def headOption[B](implicit conv: T <:< Seq[B]): Future[Option[B]] =
    future.map(_.headOption)(FutureUtils.sameThreadExecutionContext)

}


object RichFutureObj {

  def serially[T, M[X] <: TraversableOnce[X], B](coll: M[T])
                                                (fun: T => Future[B])
                                                (implicit
                                                 context: ExecutionContext,
                                                 canBuildFrom: CanBuildFrom[M[T], B, M[B]]): Future[M[B]] = {
    val init = Future.successful(canBuildFrom(coll))
    coll.foldLeft(init) { (pevFuture, current) =>
      for {
        builder <- pevFuture.fast
        next <- fun(current).fast
      } yield builder += next
    }.fast
     .map(_.result())
  }

  def handleWith[T](block: => Future[T]): Future[T] = {
    Future.fromTry(Try(block)).flatten
  }

  def handle[T](block: => T): Future[T] = {
    Future.fromTry(Try(block))
  }
}


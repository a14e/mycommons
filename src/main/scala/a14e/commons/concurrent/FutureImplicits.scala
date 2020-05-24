package a14e.commons.concurrent

import scala.language.implicitConversions
import scala.util.Try
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.http.scaladsl.util.FastFuture._
import akka.http.scaladsl.util.FastFuture
import scala.collection.BuildFrom


object FutureImplicits {
  implicit def Future2RichFuture[T](f: Future[T]): RichFuture[T] = new RichFuture[T](f)

  implicit def FutureObj2RichFuture(f: Future.type): RichFutureObj.type = RichFutureObj

  object ParasiticContext {
    implicit def parasiticExecutionContext: ExecutionContext = ExecutionContext.parasitic
  }
}

class RichFuture[T](val future: Future[T]) extends AnyVal {
  def toUnit: Future[Unit] = future.fast.map(_ => ())(FutureUtils.sameThreadExecutionContext)

  def toFast: Future[T] = FastFuture.successful(future).flatMap(identity)(FutureUtils.sameThreadExecutionContext)

  def extractTry: Future[Try[T]] = future.transform(Try(_))(FutureUtils.sameThreadExecutionContext)

  def headOption[B](implicit conv: T <:< Seq[B]): Future[Option[B]] =
    future.map(_.headOption)(FutureUtils.sameThreadExecutionContext)

}


object RichFutureObj {

  def serially[T, M[X] <: Iterable[X], B](coll: M[T])
                                         (fun: T => Future[B])
                                         (implicit
                                          canBuildFrom: BuildFrom[M[T], B, M[B]]): Future[M[B]] = {
    implicit def parasiticContext = ExecutionContext.parasitic

    val init = Future.successful(canBuildFrom(coll))
    coll.foldLeft(init) { (pevFuture, current) =>
      for {
        builder <- pevFuture.fast
        next <- fun(current).fast
      } yield builder += next
    }.fast
     .map(_.result())
  }


  def batchTraverse[T, M[X] <: Iterable[X], B](coll: M[T], batchSize: Int)
                                              (fun: T => Future[B])
                                              (implicit
                                               canBuildFrom: BuildFrom[M[T], B, M[B]]): Future[M[B]] = {
    implicit def parasiticContext = ExecutionContext.parasitic

    val init = Future.successful(canBuildFrom(coll))
    val grouped = coll.grouped(batchSize)
    grouped.foldLeft(init) { (pevFuture, group) =>
      for {
        builder <- pevFuture.fast
        next <- Future.traverse(group)(fun).fast
      } yield builder ++= next
    }.map(_.result())
  }

  def handleWith[T](block: => Future[T]): Future[T] = {
    Future.fromTry(Try(block)).flatten
  }

  def handle[T](block: => T): Future[T] = {
    Future.fromTry(Try(block))
  }
}


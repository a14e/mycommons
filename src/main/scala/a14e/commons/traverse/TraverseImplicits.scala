package a14e.commons.traverse

import cats.effect.IO
import cats.{Monad, Traverse}
import cats.implicits._

import scala.language.higherKinds

object TraverseImplicits {

  implicit class RichListTraverse(val tr: Traverse[List]) extends AnyVal {
    def serially[A, B, F[_]: Monad](col: List[A])(f: A => F[B]): F[List[B]] = {
      tr.foldLeft(col, Monad[F].pure(List.empty[B])) { (colF, x) =>
        for {
          buf <- colF
          next <- f(x)
        } yield next :: buf
      }.map(_.reverse)
    }
  }

  implicit class RichIOTraverse(val io: IO.type) extends AnyVal {
    def serially[A, B](col: Seq[A])(f: A => IO[B]): IO[List[B]] = {
      col.foldLeft(IO.pure(List.empty[B])) { (colF, x) =>
        for {
          buf <- colF
          next <- f(x)
        } yield next :: buf
      }.map(_.reverse)
    }
  }

  implicit class RichVectorTraverse(val tr: Traverse[Vector]) extends AnyVal {
    def serially[A, B, F[_]: Monad](col: Vector[A])(f: A => F[B]): F[Vector[B]] = {
      tr.foldLeft(col, Monad[F].pure(Vector.empty[B])) { (colF, x) =>
        for {
          buf <- colF
          next <- f(x)
        } yield buf :+ next
      }
    }
  }
}

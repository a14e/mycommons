package a14e.commons.catseffect

import cats.effect
import cats.effect.{Effect, Sync}

import scala.concurrent.Future

object CatsIoImplicits {

  implicit class RichSyncInstance[F[_]](val sync: Sync[F]) extends AnyVal {

    import cats.implicits._

    private implicit def implicitF = sync

    def serially[A, B](col: Seq[A])(f: A => F[B]): F[List[B]] = {
      col.foldLeft(Sync[F].pure(List.empty[B])) { (colF, x) =>
        for {
          buf <- colF
          next <- f(x)
        } yield next :: buf
      }.map(_.reverse)
    }
  }



}

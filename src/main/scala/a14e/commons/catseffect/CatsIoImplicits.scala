package a14e.commons.catseffect

import cats.effect
import cats.effect.{Effect, Sync}

import scala.concurrent.Future

object CatsIoImplicits {

  implicit class RichSyncInstance[F[_]](val sync: Sync[F]) extends AnyVal {

    import cats.implicits._

    private implicit def implicitF = sync

    def apply[B](block: => B): F[B] = Sync[F].delay(block)

    def serially[A, B](col: Seq[A])(f: A => F[B]): F[List[B]] = {
      col.foldLeft(Sync[F].pure(List.empty[B])) { (colF, x) =>
        for {
          buf <- colF
          next <- f(x)
        } yield next :: buf
      }.map(_.reverse)
    }
  }


  implicit class RichEffectRunnable[F[_], A](val io: F[A]) extends AnyVal {

    import cats.implicits._
    import cats.effect.implicits._
    def unsafeRunSync()(implicit effect: Effect[F]): A = io.toIO.unsafeRunSync()
    def unsafeToFuture()(implicit effect: Effect[F]): Future[A] = io.toIO.unsafeToFuture()
    def unsafeRunAsyncAndForget()(implicit effect: Effect[F]): Unit = io.toIO.unsafeRunAsyncAndForget()

  }

}

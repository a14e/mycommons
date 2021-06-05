package a14e.commons.catseffect

import cats.effect
import cats.effect.Sync
import cats.effect.std.Dispatcher

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
    def unsafeRunSync()(implicit d: Dispatcher[F]): A = d.unsafeRunSync(io)
    def unsafeToFuture()(implicit d: Dispatcher[F]): Future[A] = d.unsafeToFuture(io)
    def unsafeRunAsyncAndForget()(implicit d: Dispatcher[F]): Unit = d.unsafeRunAndForget(io)

  }

}

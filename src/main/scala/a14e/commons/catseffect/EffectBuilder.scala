package a14e.commons.catseffect

import cats.effect.{Async, Effect, ExitCase, IO, SyncIO}
import cats.~>

import scala.language.higherKinds

object EffectBuilder {

  def apply[F[_]: Async](toIo: F ~> IO): Effect[F] = new EffectFromAsync[F] {
    override def runAsync[A](fa: F[A])
                            (cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = {

      Effect[IO].runAsync(toIo.apply(fa))(cb)
    }
  }

}


abstract class EffectFromAsync[F[_]](implicit async: Async[F]) extends Effect[F] {

  override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

  override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

  override def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

  override def bracketCase[A, B](acquire: F[A])
                                (use: A => F[B])
                                (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] = {
    Async[F].bracketCase(acquire)(use)(release)
  }

  override def raiseError[A](e: Throwable): F[A] = {
    Async[F].raiseError(e)
  }

  override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = {
    Async[F].handleErrorWith(fa)(f)
  }

  override def pure[A](x: A): F[A] = {
    Async[F].pure(x)
  }

  override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

  override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)
}
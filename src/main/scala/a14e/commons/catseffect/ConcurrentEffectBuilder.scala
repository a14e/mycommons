package a14e.commons.catseffect

import cats.effect.{Async, CancelToken, Concurrent, ConcurrentEffect, Effect, Fiber, IO, SyncIO}
import cats.~>

import scala.util.Either

object ConcurrentEffectBuilder {
  def apply[F[_] : Concurrent : Effect](toIo: F ~> IO)
                                       (implicit ioConcurEvent: ConcurrentEffect[IO]): ConcurrentEffect[F] = new ConcurrentEffectFromConcAndEffect[F] {

    override def runCancelable[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[F]] = {
      // TODO проверить
      ConcurrentEffect[IO].runCancelable(toIo(fa))(cb)
        .map { token =>
          Concurrent[F].async { cb =>
            token.unsafeRunCancelable(cb)
          }
        }
    }
  }

}


abstract class ConcurrentEffectFromConcAndEffect[F[_] : Concurrent : Effect] extends EffectFromAsync[F]()(async = Concurrent[F]) with ConcurrentEffect[F] {

  def runCancelable[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[F]]

  override def start[A](fa: F[A]): F[Fiber[F, A]] = Concurrent[F].start(fa)

  override def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]] = {
    Concurrent[F].racePair(fa, fb)
  }

  override def runAsync[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = {
    Effect[F].runAsync(fa)(cb)
  }
}
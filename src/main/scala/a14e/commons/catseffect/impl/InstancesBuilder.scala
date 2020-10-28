package a14e.commons.catseffect.impl

import cats.effect.{Async, CancelToken, Concurrent, ConcurrentEffect, Effect, ExitCase, Fiber, IO, SyncIO}

import scala.language.higherKinds

object InstancesBuilder {

  def buildConcurrentEffect[F[_] : Async](cStarter: ConcurrentStarter[F],
                                          effectRun: EffectRun[F],
                                          runCancellable: RunCancellable[F]): ConcurrentEffect[F] = new ConcurrentEffect[F] {
    override def runCancelable[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[F]] = {
      runCancellable.runCancelable(fa)(cb)
    }

    override def runAsync[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = effectRun.runAsync(fa)(cb)

    override def start[A](fa: F[A]): F[Fiber[F, A]] = cStarter.start(fa)

    override def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]] =
      cStarter.racePair(fa, fb)

    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

    override def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

    override def bracketCase[A, B](acquire: F[A])
                                  (use: A => F[B])
                                  (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      Async[F].bracketCase(acquire)(use)(release)

    override def raiseError[A](e: Throwable): F[A] = Async[F].raiseError(e)

    override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = Async[F].handleErrorWith(fa)(f)

    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

    override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)

    override def pure[A](x: A): F[A] = Async[F].pure(x)
  }


  def buildEffect[F[_] : Async](effectRun: EffectRun[F]): Effect[F] = new Effect[F] {

    override def runAsync[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = effectRun.runAsync(fa)(cb)

    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

    override def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

    override def bracketCase[A, B](acquire: F[A])
                                  (use: A => F[B])
                                  (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      Async[F].bracketCase(acquire)(use)(release)

    override def raiseError[A](e: Throwable): F[A] = Async[F].raiseError(e)

    override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = Async[F].handleErrorWith(fa)(f)

    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

    override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)

    override def pure[A](x: A): F[A] = Async[F].pure(x)
  }

  def buildConcurrent[F[_] : Async](cStarter: ConcurrentStarter[F]): Concurrent[F] = new Concurrent[F] {

    override def start[A](fa: F[A]): F[Fiber[F, A]] = cStarter.start(fa)

    override def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]] =
      cStarter.racePair(fa, fb)

    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

    override def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

    override def bracketCase[A, B](acquire: F[A])
                                  (use: A => F[B])
                                  (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      Async[F].bracketCase(acquire)(use)(release)

    override def raiseError[A](e: Throwable): F[A] = Async[F].raiseError(e)

    override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = Async[F].handleErrorWith(fa)(f)

    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

    override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)

    override def pure[A](x: A): F[A] = Async[F].pure(x)
  }
}

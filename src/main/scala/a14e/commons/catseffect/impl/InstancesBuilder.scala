package a14e.commons.catseffect.impl

import cats.effect.{Async, CancelToken, Concurrent, ConcurrentEffect, Effect, ExitCase, Fiber, IO, SyncIO}

import scala.language.higherKinds

object InstancesBuilder {

  def buildConcurrentEffect[F[_] : Async](concurMethods: ConcurrentMethods[F],
                                          effectMethods: EffectMethods[F],
                                          concurrentEffectMethods: ConcurrentEffectMethods[F]): ConcurrentEffect[F] = new ConcurrentEffect[F] {
    override final def runCancelable[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[F]] = {
      concurrentEffectMethods.runCancelable(fa)(cb)
    }

    override final def runAsync[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = effectMethods.runAsync(fa)(cb)

    override final def start[A](fa: F[A]): F[Fiber[F, A]] = concurMethods.start(fa)

    override final def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]] =
      concurMethods.racePair(fa, fb)

    override final def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

    override final def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

    override final def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

    override final def bracketCase[A, B](acquire: F[A])
                                  (use: A => F[B])
                                  (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      Async[F].bracketCase(acquire)(use)(release)

    override final def raiseError[A](e: Throwable): F[A] = Async[F].raiseError(e)

    override final def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = Async[F].handleErrorWith(fa)(f)

    override final def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

    override final def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)

    override final def pure[A](x: A): F[A] = Async[F].pure(x)
  }


  def buildEffect[F[_] : Async](effectMethods: EffectMethods[F]): Effect[F] = new Effect[F] {

    override final def runAsync[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = effectMethods.runAsync(fa)(cb)

    override final def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

    override final def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

    override final def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

    override final def bracketCase[A, B](acquire: F[A])
                                  (use: A => F[B])
                                  (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      Async[F].bracketCase(acquire)(use)(release)

    override final def raiseError[A](e: Throwable): F[A] = Async[F].raiseError(e)

    override final def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = Async[F].handleErrorWith(fa)(f)

    override final def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

    override final def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)

    override final def pure[A](x: A): F[A] = Async[F].pure(x)
  }

  def buildConcurrent[F[_] : Async](concurrentMethods: ConcurrentMethods[F]): Concurrent[F] = new Concurrent[F] {

    override final def start[A](fa: F[A]): F[Fiber[F, A]] = concurrentMethods.start(fa)

    override final def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]] =
      concurrentMethods.racePair(fa, fb)

    override final def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] = Async[F].async(k)

    override final def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = Async[F].asyncF(k)

    override final def suspend[A](thunk: => F[A]): F[A] = Async[F].suspend(thunk)

    override final def bracketCase[A, B](acquire: F[A])
                                  (use: A => F[B])
                                  (release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      Async[F].bracketCase(acquire)(use)(release)

    override final def raiseError[A](e: Throwable): F[A] = Async[F].raiseError(e)

    override final def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = Async[F].handleErrorWith(fa)(f)

    override final def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Async[F].flatMap(fa)(f)

    override final def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = Async[F].tailRecM(a)(f)

    override final def pure[A](x: A): F[A] = Async[F].pure(x)
  }
}

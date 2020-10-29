package a14e.commons.catseffect.impl

import a14e.commons.catseffect.ValueBuilder
import a14e.commons.catseffect.impl.EffectMethods.fromArrow
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{Concurrent, Effect, ExitCase, Fiber, IO, Sync}
import cats.{Applicative, ~>}

// telper to build ConcurrentStarter
trait ConcurrentMethods[F[_]] {
  def start[A](fa: F[A]): F[Fiber[F, A]]

  def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]]
}


object ConcurrentMethods {

  def fromConcurrent[F[_]: Concurrent]: ConcurrentMethods[F] = new ConcurrentMethods[F] {
    override def start[A](fa: F[A]): F[Fiber[F, A]] = Concurrent[F].start(fa)

    override def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(A, Fiber[F, B]), (Fiber[F, A], B)]] =
      Concurrent[F].racePair(fa, fb)
  }

  // на форках не добавляется контекст
  def stateT[F[_] : Concurrent, CTX]: ConcurrentMethods[StateT[F, CTX, *]] =
    new ConcurrentMethods[StateT[F, CTX, *]] {
      type OUTER[A] = StateT[F, CTX, A]

      import cats.implicits._


      override def start[A](fa: OUTER[A]): OUTER[Fiber[OUTER, A]] = {
        StateT[F, CTX, Fiber[OUTER, A]] { ctx =>
          Concurrent[F].start(fa.runA(ctx))
            .map { fiber =>
              fiber.mapK(Arrows.stateT[F, CTX])
            }.map(x => ctx -> x)
        }
      }

      override def racePair[A, B](fa: OUTER[A],
                                  fb: OUTER[B]): OUTER[Either[(A, Fiber[OUTER, B]), (Fiber[OUTER, A], B)]] = {

        def mapFiber[T, C](fiber: Fiber[OUTER, T])(func: T => C): Fiber[OUTER, C] = {
          Fiber(Applicative[OUTER].map(fiber.join)(func), fiber.cancel)
        }

        def convertFiberToOuter[T](fiber: Fiber[F, (CTX, T)]): Fiber[OUTER, T] = {
          mapFiber(fiber.mapK(Arrows.stateT[F, CTX])) { case (_, b) => b }
        }

        StateT { ctx =>
          Concurrent[F].racePair(fa.run(ctx), fb.run(ctx))
            .map {
              case Left(((ctx, a), fiberB)) =>
                val newFiber = convertFiberToOuter(fiberB)
                ctx -> Left((a, newFiber))
              case Right((fiberA, (ctx, b))) =>
                val newFiber = convertFiberToOuter(fiberA)
                ctx -> Right((newFiber, b))
            }
        }
      }

    }
}

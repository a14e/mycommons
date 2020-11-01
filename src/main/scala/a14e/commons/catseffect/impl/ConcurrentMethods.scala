package a14e.commons.catseffect.impl

import a14e.commons.catseffect.impl.EffectMethods.fromArrow
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{CancelToken, Concurrent, Effect, ExitCase, Fiber, IO, Sync}
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

  def stateT[F[_] : Concurrent, CTX](merge: (CTX, CTX) => CTX): ConcurrentMethods[StateT[F, CTX, *]] =
    new ConcurrentMethods[StateT[F, CTX, *]] {
      type OUTER[A] = StateT[F, CTX, A]

      import cats.implicits._


      override def start[A](fa: OUTER[A]): OUTER[Fiber[OUTER, A]] = {
        StateT[F, CTX, Fiber[OUTER, A]] { ctx =>
          Concurrent[F].start(fa.run(ctx))
            .map(fiberToStateT)
            .map(x => ctx -> x)
        }
      }

      override def racePair[A, B](fa: OUTER[A],
                                  fb: OUTER[B]): OUTER[Either[(A, Fiber[OUTER, B]), (Fiber[OUTER, A], B)]] = {
        StateT { ctx =>
          Concurrent[F].racePair(fa.run(ctx), fb.run(ctx))
            .map {
              case Left(((ctx, a), fiberB)) =>
                val newFiber = fiberToStateT(fiberB)
                ctx -> Left((a, newFiber))
              case Right((fiberA, (ctx, b))) =>
                val newFiber = fiberToStateT(fiberA)
                ctx -> Right((newFiber, b))
            }
        }
      }

      private def fiberToStateT[A](fiber: Fiber[F, (CTX, A)]): Fiber[OUTER, A] = {
        val f = Arrows.stateT[F, CTX]
        new Fiber[OUTER, A] {
          def cancel: CancelToken[OUTER] = f(fiber.cancel)
          def join: OUTER[A] = {
            StateT { oldCtx =>
              fiber.join.map { case  (updatedCtx, a) =>
                val ctx = merge(oldCtx, updatedCtx)
                ctx -> a
              }
            }
          }
        }
      }
    }



}

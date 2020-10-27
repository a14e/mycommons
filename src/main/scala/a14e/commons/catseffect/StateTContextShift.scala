package a14e.commons.catseffect

import cats.data.StateT
import cats.effect.{ContextShift, Sync}

import scala.concurrent.ExecutionContext


class StateTContextShift[F[_] : Sync, CTX](underlying: ContextShift[F]) extends ContextShift[StateT[F, CTX, *]] {
  type STATE[T] = StateT[F, CTX, T]

  override def shift: STATE[Unit] = StateT.liftF[F, CTX, Unit](underlying.shift)

  override def evalOn[A](ec: ExecutionContext)(fa: STATE[A]): STATE[A] = {
    for {
      ctx <- StateT.get[F, CTX]
      (nexCtx, res) <- StateT.liftF[F, CTX, (CTX, A)] {
        underlying.evalOn(ec) {
          fa.run(ctx)
        }
      }
      _ <- StateT.set[F, CTX](nexCtx)
    } yield res
  }
}
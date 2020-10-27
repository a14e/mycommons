package a14e.commons.catseffect

import cats.data.{ReaderT, StateT}
import cats.effect.{ContextShift, Sync}

import scala.concurrent.ExecutionContext


class ReaderTContextShift[F[_] : Sync, CTX](underlying: ContextShift[F]) extends ContextShift[ReaderT[F, CTX, *]] {
  type READER[T] = ReaderT[F, CTX, T]

  override def shift: READER[Unit] = ReaderT.liftF[F, CTX, Unit](underlying.shift)

  override def evalOn[A](ec: ExecutionContext)(fa: READER[A]): READER[A] = {
    for {
      ctx <- ReaderT.ask[F, CTX]
      res <- ReaderT.liftF[F, CTX, A] {
        underlying.evalOn(ec) {
          fa.apply(ctx)
        }
      }
    } yield res
  }
}


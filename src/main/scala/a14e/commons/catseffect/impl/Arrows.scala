package a14e.commons.catseffect.impl

import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.{Applicative, ~>}

object Arrows {

  def readerT[F[_]: Applicative, CTX]:  F ~> ReaderT[F, CTX, *] = new FunctionK[F, ReaderT[F, CTX, *]] {
    override def apply[A](fa: F[A]): ReaderT[F, CTX, A] = ReaderT.liftF(fa)
  }

  def stateT[F[_]: Applicative, CTX]:  F ~> StateT[F, CTX, *] = new FunctionK[F, StateT[F, CTX, *]] {
    override def apply[A](fa: F[A]): StateT[F, CTX, A] = StateT.liftF(fa)
  }
}

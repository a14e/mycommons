package a14e.commons.catseffect.impl

import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.Sync
import cats.{Applicative, ~>}
import scala.language.higherKinds

object Arrows {

  def readerT[F[_]: Applicative, CTX]:  F ~> ReaderT[F, CTX, *] = new FunctionK[F, ReaderT[F, CTX, *]] {
    override def apply[A](fa: F[A]): ReaderT[F, CTX, A] = ReaderT.liftF(fa)
  }

  def stateT[F[_]: Applicative, CTX]:  F ~> StateT[F, CTX, *] = new FunctionK[F, StateT[F, CTX, *]] {
    override def apply[A](fa: F[A]): StateT[F, CTX, A] = StateT.liftF(fa)
  }

  import cats.implicits._
  object fromInit {
    def readerT[F[_] : Sync, CTX](init: () => F[CTX]): ReaderT[F, CTX, *] ~> F = new FunctionK[ReaderT[F, CTX, *], F] {
      override def apply[A](fa: ReaderT[F, CTX, A]): F[A] =
        for {
          init <- Sync[F].suspend(init())
          res <- fa.run(init)
        } yield res
    }

    def stateT[F[_] : Sync, CTX](init: () => F[CTX]): StateT[F, CTX, *] ~> F = new FunctionK[StateT[F, CTX, *], F] {
      override def apply[A](fa: StateT[F, CTX, A]): F[A] =
        for {
          init <- Sync[F].suspend(init())
          res <- fa.runA(init)
        } yield res
    }
  }
}

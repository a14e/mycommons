package a14e.commons.context

import a14e.commons.catseffect.EffectBuilder
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{Effect, IO}
import cats.~>

object ContextEffect {

  def readerT[CTX](init: () => IO[CTX]): Effect[ReaderT[IO, CTX, *]] = {
    EffectBuilder.apply(readerTToIOArrow(init))
  }

  def stateT[CTX](init: () => IO[CTX]): Effect[StateT[IO, CTX, *]] = {
    EffectBuilder.apply(stateTToIOArrow(init))
  }

  def readerTToIOArrow[CTX](init: () => IO[CTX]): FunctionK[ReaderT[IO, CTX, *], IO] = new FunctionK[ReaderT[IO, CTX, *], IO] {
    override def apply[A](fa: ReaderT[IO, CTX, A]): IO[A] = {
      for {
        ctx <- init()
        result <- fa.run(ctx)
      } yield result
    }
  }

  def stateTToIOArrow[CTX](init: () => IO[CTX]): FunctionK[StateT[IO, CTX, *], IO] = new FunctionK[StateT[IO, CTX, *], IO] {
    override def apply[A](fa: StateT[IO, CTX, A]): IO[A] = {
      for {
        ctx <- init()
        (_, result) <- fa.run(ctx)
      } yield result
    }
  }

}

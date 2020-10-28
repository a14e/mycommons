package a14e.commons.catseffect

import a14e.commons.catseffect.impl.{ConcurrentMethods, EffectMethods, ConcurrentEffectMethods}
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.Sync
import cats.~>

trait ValueBuilder[F[_], CTX] {

  def startValue(): F[CTX]
}

object ValueBuilder {
  def apply[F[_], CTX](build: => F[CTX]): ValueBuilder[F, CTX] = new ValueBuilder[F, CTX] {
    override def startValue(): F[CTX] = build
  }

  def of[F[_], CTX](implicit valuerBuilder: ValueBuilder[F, CTX]): ValueBuilder[F, CTX] = valuerBuilder


  import cats.implicits._

  def readerT[F[_] : Sync, CTX](implicit valuerBuilder: ValueBuilder[F, CTX]): ReaderT[F, CTX, *] ~> F = new FunctionK[ReaderT[F, CTX, *], F] {
    override def apply[A](fa: ReaderT[F, CTX, A]): F[A] =
      for {
        init <- Sync[F].suspend(ValueBuilder.of[F, CTX].startValue())
        res <- fa.run(init)
      } yield res
  }


  def stateT[F[_] : Sync, CTX](implicit valuerBuilder: ValueBuilder[F, CTX]): StateT[F, CTX, *] ~> F = new FunctionK[StateT[F, CTX, *], F] {
    override def apply[A](fa: StateT[F, CTX, A]): F[A] =
      for {
        init <- Sync[F].suspend(ValueBuilder.of[F, CTX].startValue())
        res <- fa.runA(init)
      } yield res
  }


}

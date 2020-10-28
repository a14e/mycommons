package a14e.commons.catseffect

import a14e.commons.catseffect.ValueBuilder.DummyStarter
import a14e.commons.catseffect.impl.{ConcurrentStarter, EffectRun, RunCancellable}
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.Sync
import cats.~>

trait ValueBuilder[F[_], CTX] {

  def startValue(): F[CTX]
}

object ValueBuilder {
  def apply[F[_]]: DummyStarter[F] = new DummyStarter[F]

  class DummyStarter[F[_]](private val dummyValue: Boolean = false) {
    def apply[CTX](build: => F[CTX]): ValueBuilder[F, CTX] = new ValueBuilder[F, CTX] {
      override def startValue(): F[CTX] = build
    }

    def startValue[CTX]()(implicit builder: ValueBuilder[F, CTX]): F[CTX] = builder.startValue()
  }


  import cats.implicits._

  def readerT[F[_] : Sync, CTX](implicit valuerBuilder: ValueBuilder[F, CTX]): ReaderT[F, CTX, *] ~> F = new FunctionK[ReaderT[F, CTX, *], F] {
    override def apply[A](fa: ReaderT[F, CTX, A]): F[A] = for {
      init <- Sync[F].suspend(ValueBuilder[F].startValue[CTX]())
      res <- fa.run(init)
    } yield res
  }


  def stateT[F[_] : Sync, CTX](implicit valuerBuilder: ValueBuilder[F, CTX]): StateT[F, CTX, *] ~> F = new FunctionK[StateT[F, CTX, *], F] {
    override def apply[A](fa: StateT[F, CTX, A]): F[A] = for {
      init <- Sync[F].suspend(ValueBuilder[F].startValue[CTX]())
      res <- fa.runA(init)
    } yield res
  }


}

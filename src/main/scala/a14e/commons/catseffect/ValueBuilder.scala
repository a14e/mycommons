package a14e.commons.catseffect

import a14e.commons.catseffect.impl.{ConcurrentEffectMethods, ConcurrentMethods, EffectMethods}
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{Concurrent, ConcurrentEffect, Effect, Sync}
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
  import a14e.commons.catseffect.instances

  trait concurrenteffect {
    implicit def stateTConcurrentEffectInstance[F[_] : ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): ConcurrentEffect[StateT[F, CTX, *]] = {
      instances.concurrenteffect.stateTConcurrentEffectInstance(valuerBuilder.startValue)
    }

    def readerTConcurrentEffectInstance[F[_] : ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): ConcurrentEffect[ReaderT[F, CTX, *]] = {
      instances.concurrenteffect.readerTConcurrentEffectInstance(valuerBuilder.startValue)
    }
  }

  object concurrenteffect extends concurrenteffect

  trait effect {
    def stateTEffectInstance[F[_] : ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): Effect[StateT[F, CTX, *]] = {
      instances.effect.stateTEffectInstance(valuerBuilder.startValue)
    }

    def readerTEffectInstance[F[_] : ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): Effect[ReaderT[F, CTX, *]] = {
      instances.effect.readerTEffectInstance(valuerBuilder.startValue)
    }
  }

  object effect extends effect


}

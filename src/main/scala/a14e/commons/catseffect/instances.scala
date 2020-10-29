package a14e.commons.catseffect

import a14e.commons.catseffect.impl.{ConcurrentMethods, EffectMethods, InstancesBuilder, ConcurrentEffectMethods}
import cats.data.{ReaderT, StateT}
import cats.effect.{Async, Concurrent, ConcurrentEffect, Effect}

object instances {

  trait concurrenteffect {
    def stateTConcurrentEffectInstance[F[_]: ConcurrentEffect, CTX](init: () => F[CTX]): ConcurrentEffect[StateT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from ConcurrentEffect
      val concurrentMethods = ConcurrentMethods.stateT[F, CTX]
      val effectMethods = EffectMethods.stateT[F, CTX](init)
      val concurrentEffectMethods = ConcurrentEffectMethods.stateT[F, CTX](init)

      InstancesBuilder.buildConcurrentEffect[StateT[F, CTX, *]](concurrentMethods, effectMethods, concurrentEffectMethods)
    }

    def readerTConcurrentEffectInstance[F[_]: ConcurrentEffect, CTX](init: () => F[CTX]): ConcurrentEffect[ReaderT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from ConcurrentEffect
      val concurrentMethods = ConcurrentMethods.fromConcurrent[ReaderT[F, CTX, *]]
      val effectMethods = EffectMethods.readerT[F, CTX](init)
      val concurrentEffectMethods = ConcurrentEffectMethods.readerT[F, CTX](init)

      InstancesBuilder.buildConcurrentEffect[ReaderT[F, CTX, *]](concurrentMethods, effectMethods, concurrentEffectMethods)
    }
  }

  object concurrenteffect extends concurrenteffect

  trait effect {
    def stateTEffectInstance[F[_]: ConcurrentEffect, CTX](init: () => F[CTX]): Effect[StateT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from Effect
      val effectMethods = EffectMethods.stateT[F, CTX](init)
      InstancesBuilder.buildEffect[StateT[F, CTX, *]](effectMethods)
    }

    def readerTEffectInstance[F[_]: ConcurrentEffect, CTX](init: () => F[CTX]): Effect[ReaderT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from Effect
      val effectMethods = EffectMethods.readerT[F, CTX](init)
      InstancesBuilder.buildEffect[ReaderT[F, CTX, *]](effectMethods)
    }
  }

  object effect extends effect


  trait concurrent {
    implicit def stateTConcurrentnstance[F[_]: Concurrent, CTX]: Concurrent[StateT[F, CTX, *]] = {
      // helper for functions from Concurrent
      val concurrentMethods = ConcurrentMethods.stateT[F, CTX]
      InstancesBuilder.buildConcurrent[StateT[F, CTX, *]](concurrentMethods)
    }

  }

  object concurrent extends concurrent
}

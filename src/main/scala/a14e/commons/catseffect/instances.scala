package a14e.commons.catseffect

import a14e.commons.catseffect.impl.{ConcurrentMethods, EffectMethods, InstancesBuilder, ConcurrentEffectMethods}
import cats.data.{ReaderT, StateT}
import cats.effect.{Async, Concurrent, ConcurrentEffect, Effect}

object instances {

  trait concurrenteffect {
    implicit def stateTConcurrentEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): ConcurrentEffect[StateT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from ConcurrentEffect
      val concurrentMethods = ConcurrentMethods.stateT[F, CTX]
      val effectMethods = EffectMethods.stateT[F, CTX]
      val concurrentEffectMethods = ConcurrentEffectMethods.stateT[F, CTX]

      InstancesBuilder.buildConcurrentEffect[StateT[F, CTX, *]](concurrentMethods, effectMethods, concurrentEffectMethods)
    }

    implicit def readerTConcurrentEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): ConcurrentEffect[ReaderT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from ConcurrentEffect
      val concurrentMethods = ConcurrentMethods.readerT[F, CTX]
      val effectMethods = EffectMethods.readerT[F, CTX]
      val concurrentEffectMethods = ConcurrentEffectMethods.readerT[F, CTX]

      InstancesBuilder.buildConcurrentEffect[ReaderT[F, CTX, *]](concurrentMethods, effectMethods, concurrentEffectMethods)
    }
  }

  object concurrenteffect extends concurrenteffect

  trait effect {
    implicit def stateTEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): Effect[StateT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from Effect
      val effectMethods = EffectMethods.stateT[F, CTX]
      InstancesBuilder.buildEffect[StateT[F, CTX, *]](effectMethods)
    }

    implicit def readerTEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): Effect[ReaderT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from Effect
      val effectMethods = EffectMethods.readerT[F, CTX]
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

    implicit def readerTConcurrentInstance[F[_]: Concurrent, CTX]: Concurrent[ReaderT[F, CTX, *]] = {
      // helper for functions from Concurrent
      val concurrentMethods = ConcurrentMethods.readerT[F, CTX]
      InstancesBuilder.buildConcurrent[ReaderT[F, CTX, *]](concurrentMethods)
    }
  }

  object concurrent extends concurrent
}

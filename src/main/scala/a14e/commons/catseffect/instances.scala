package a14e.commons.catseffect

import a14e.commons.catseffect.impl.{ConcurrentStarter, EffectRun, InstancesBuilder, RunCancellable}
import cats.data.{ReaderT, StateT}
import cats.effect.{Async, Concurrent, ConcurrentEffect, Effect}

object instances {

  trait concurrenteffect {
    implicit def stateTConcurrentEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): ConcurrentEffect[StateT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from ConcurrentEffect
      val starter = ConcurrentStarter.stateT[F, CTX]
      val effectRun = EffectRun.stateT[F, CTX]
      val runCancellable = RunCancellable.stateT[F, CTX]

      InstancesBuilder.buildConcurrentEffect[StateT[F, CTX, *]](starter, effectRun, runCancellable)
    }

    implicit def readerTConcurrentEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): ConcurrentEffect[ReaderT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from ConcurrentEffect
      val starter = ConcurrentStarter.readerT[F, CTX]
      val effectRun = EffectRun.readerT[F, CTX]
      val runCancellable = RunCancellable.readerT[F, CTX]

      InstancesBuilder.buildConcurrentEffect[ReaderT[F, CTX, *]](starter, effectRun, runCancellable)
    }
  }

  object concurrenteffect extends concurrenteffect

  trait effect {
    implicit def stateTEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): Effect[StateT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from Effect
      val effectRun = EffectRun.stateT[F, CTX]
      InstancesBuilder.buildEffect[StateT[F, CTX, *]](effectRun)
    }

    implicit def readerTEffectInstance[F[_]: ConcurrentEffect, CTX]
    (implicit valuerBuilder: ValueBuilder[F, CTX]): Effect[ReaderT[F, CTX, *]] = {
      // just list of helpers for 1-3 functions from Effect
      val effectRun = EffectRun.readerT[F, CTX]
      InstancesBuilder.buildEffect[ReaderT[F, CTX, *]](effectRun)
    }
  }

  object effect extends effect


  trait concurrent {
    implicit def stateTConcurrentnstance[F[_]: Concurrent, CTX]: Concurrent[StateT[F, CTX, *]] = {
      // helper for functions from Concurrent
      val starter = ConcurrentStarter.stateT[F, CTX]
      InstancesBuilder.buildConcurrent[StateT[F, CTX, *]](starter)
    }

    implicit def readerTConcurrentInstance[F[_]: Concurrent, CTX]: Concurrent[ReaderT[F, CTX, *]] = {
      // helper for functions from Concurrent
      val starter = ConcurrentStarter.readerT[F, CTX]
      InstancesBuilder.buildConcurrent[ReaderT[F, CTX, *]](starter)
    }
  }

  object concurrent extends concurrent
}

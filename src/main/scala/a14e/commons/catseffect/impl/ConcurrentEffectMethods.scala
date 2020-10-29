package a14e.commons.catseffect.impl

import a14e.commons.catseffect.ValueBuilder
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{CancelToken, Concurrent, ConcurrentEffect, Effect, ExitCase, Fiber, IO, Sync, SyncIO}
import cats.~>

import scala.language.higherKinds
import scala.util.Either

// HELPER to build ConcurrentEffect
trait ConcurrentEffectMethods[F[_]] {
  def runCancelable[A](fa: F[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[F]]
}

object ConcurrentEffectMethods {

  def fromArrow[F[_] : ConcurrentEffect : Sync, CTX, B[_]](to: F ~> B,
                                                         from: B ~> F): ConcurrentEffectMethods[B] = new ConcurrentEffectMethods[B] {

    override def runCancelable[A](fa: B[A])
                                 (cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[B]] = {
      ConcurrentEffect[F].runCancelable(from(fa))(cb).map((res: CancelToken[F]) => to.apply(res))
    }
  }

  def readerT[F[_] : ConcurrentEffect : Sync, CTX](init: () => F[CTX]): ConcurrentEffectMethods[ReaderT[F, CTX, *]] = {
    type OUTER[A] = ReaderT[F, CTX, A]
    val to: F ~> OUTER = Arrows.readerT[F, CTX]
    val from: OUTER ~> F = Arrows.fromInit.readerT[F, CTX](init)

    fromArrow(to, from)
  }

  def stateT[F[_] : ConcurrentEffect : Sync, CTX](init: () => F[CTX]): ConcurrentEffectMethods[StateT[F, CTX, *]] = {
    type OUTER[A] = StateT[F, CTX, A]
    val to: F ~> OUTER = Arrows.stateT[F, CTX]
    val from: OUTER ~> F = Arrows.fromInit.stateT[F, CTX](init: () => F[CTX])

    fromArrow(to, from)
  }
}


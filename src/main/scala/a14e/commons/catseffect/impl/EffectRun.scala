package a14e.commons.catseffect.impl

import a14e.commons.catseffect.ValueBuilder
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{Async, Effect, ExitCase, IO, Sync, SyncIO}
import cats.~>

import scala.language.higherKinds
import scala.util.Either

// HELPER to build Effect
trait EffectRun[F[_]] {
  def runAsync[A](fa: F[A])
                 (cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit]

}

object EffectRun {

  def fromArrow[F[_] : Effect : Sync, CTX, B[_]](arrow: B ~> F): EffectRun[B] = new EffectRun[B] {
    def runAsync[A](fa: B[A])
                   (cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = {
      Effect[F].runAsync(arrow(fa))(cb)
    }
  }

  def readerT[F[_] : Effect : Sync, CTX](implicit startValueBuilder: ValueBuilder[F, CTX]): EffectRun[ReaderT[F, CTX, *]] = {
    fromArrow(ValueBuilder.readerT[F, CTX])
  }

  def stateT[F[_] : Effect : Sync, CTX](implicit startValueBuilder: ValueBuilder[F, CTX]): EffectRun[StateT[F, CTX, *]] = {
    fromArrow(ValueBuilder.stateT[F, CTX])
  }

}



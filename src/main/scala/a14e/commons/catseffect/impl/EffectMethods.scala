package a14e.commons.catseffect.impl

import a14e.commons.catseffect.ValueBuilder
import cats.arrow.FunctionK
import cats.data.{ReaderT, StateT}
import cats.effect.{Async, Effect, ExitCase, IO, Sync, SyncIO}
import cats.~>

import scala.language.higherKinds
import scala.util.Either

// HELPER to build Effect
trait EffectMethods[F[_]] {
  def runAsync[A](fa: F[A])
                 (cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit]

}

object EffectMethods {

  def fromArrow[F[_] : Effect : Sync, CTX, B[_]](arrow: B ~> F): EffectMethods[B] = new EffectMethods[B] {
    def runAsync[A](fa: B[A])
                   (cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = {
      Effect[F].runAsync(arrow(fa))(cb)
    }
  }

  def readerT[F[_] : Effect : Sync, CTX](implicit startValueBuilder: ValueBuilder[F, CTX]): EffectMethods[ReaderT[F, CTX, *]] = {
    fromArrow(ValueBuilder.readerT[F, CTX])
  }

  def stateT[F[_] : Effect : Sync, CTX](implicit startValueBuilder: ValueBuilder[F, CTX]): EffectMethods[StateT[F, CTX, *]] = {
    fromArrow(ValueBuilder.stateT[F, CTX])
  }

}



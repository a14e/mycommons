package a14e.commons.context

import a14e.commons.mdc.MdcEffect
import cats.{Applicative, FlatMap, Monad}
import cats.data.{ReaderT, StateT}
import cats.effect.Sync
import cats.implicits._

trait ContextualSet[F[_]] {
  def set(ctx: Contextual.Context): F[Unit]
}

object ContextualSet {
  def apply[F[_] : ContextualSet]: ContextualSet[F] = implicitly[ContextualSet[F]]

  def stateT[F[_] : Monad, CTX](merge: (CTX, Contextual.Context) => CTX): ContextualSet[StateT[F, CTX, *]] = {
    additionalCtx => {
      for {
        oldCtx <- StateT.get[F, CTX]
        newCtx = merge(oldCtx, additionalCtx)
        _ <- StateT.set[F, CTX](newCtx)
      } yield ()
    }
  }

  def mdcContextSet[F[_] : Sync]: ContextualSet[F] = {
    import cats.implicits._
    import scala.jdk.CollectionConverters._
    mdc => MdcEffect.setMdc(mdc.asJava)[F]
  }

}

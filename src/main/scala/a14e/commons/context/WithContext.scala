package a14e.commons.context

import cats.Monad
import cats.data.{ReaderT, StateT}

trait WithContext[F[_]] {
  def withContextMap[T](f: Contextual.Context => Contextual.Context)
                       (body: => F[T]): F[T]

  def withContext[T](ctx: Contextual.Context)
                    (body: => F[T]): F[T] = withContextMap(_ => ctx)(body)
}


object WithContext {

  def apply[F[_] : WithContext]: WithContext[F] = implicitly[WithContext[F]]

  def stateT[F[_] : Monad, CTX](getCtx: CTX => Contextual.Context,
                                merge: (CTX, Contextual.Context) => CTX): WithContext[StateT[F, CTX, *]] = {
    new WithContext[StateT[F, CTX, *]] {
      override def withContextMap[T](f: Contextual.Context => Contextual.Context)
                                    (body: => StateT[F, CTX, T]): StateT[F, CTX, T] = {
        for {
          oldCtx <- StateT.get[F, CTX]
          _ <- StateT.modify[F, CTX] { ctx =>
            merge(ctx, f(getCtx(ctx)))
          }
          res <- body
          _ <- StateT.set(oldCtx)
        } yield res
      }
    }
  }


  def readerT[F[_], CTX](getCtx: CTX => Contextual.Context,
                         merge: (CTX, Contextual.Context) => CTX): WithContext[ReaderT[F, CTX, *]] = {
    new WithContext[ReaderT[F, CTX, *]] {
      override def withContextMap[T](f: Contextual.Context => Contextual.Context)
                                    (body: => ReaderT[F, CTX, T]): ReaderT[F, CTX, T] = {
        body.local[CTX] { state =>
          val oldCtx = getCtx(state)
          val updatedCtx = f(oldCtx)
          val newStats = merge(state, updatedCtx)
          newStats
        }
      }
    }
  }
}

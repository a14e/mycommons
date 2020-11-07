package a14e.commons.context

import cats.Monad
import cats.data.{ReaderT, StateT}
import cats.effect.{IO, Sync}

trait WithContext[F[_]] {
  def withContextMap[T](f: Contextual.Context => Contextual.Context)(body: => F[T]): F[T]

  def withContext[T](ctx: Contextual.Context)(body: => F[T]): F[T] = withContextMap(_ => ctx)(body)
}


object WithContext {

  def apply[F[_] : WithContext]: WithContext[F] = implicitly[WithContext[F]]

  def stateT[F[_] : Sync]: WithContext[StateT[F, Contextual.Context, *]] = {
    new WithContext[StateT[F, Contextual.Context, *]] {
      override def withContextMap[T](f: Contextual.Context => Contextual.Context)
                                    (body: => StateT[F, Contextual.Context, T]): StateT[F, Contextual.Context, T] = {
        for {
          oldCtx <- StateT.get[F, Contextual.Context]
          _ <- StateT.modify(f)
          res <- body
          _ <- StateT.set(oldCtx)
        } yield res
      }
    }
  }


  def readerT[F[_] : Sync]: WithContext[ReaderT[F, Contextual.Context, *]] = {
    new WithContext[ReaderT[F, Contextual.Context, *]] {
      override def withContextMap[T](f: Contextual.Context => Contextual.Context)
                                    (body: => ReaderT[F, Contextual.Context, T]): ReaderT[F, Contextual.Context, T] = {
        body.local(f)
      }
    }
  }
}

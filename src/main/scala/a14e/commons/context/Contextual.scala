package a14e.commons.context

import a14e.commons.context.Contextual.Context
import cats.{Applicative, Functor}
import cats.data.{ReaderT, StateT, Writer, WriterT}
import cats.effect.{Fiber, IO, Sync, SyncIO}
import com.typesafe.scalalogging.Logger
import org.slf4j.Marker

import scala.concurrent.ExecutionContext

trait Contextual[F[_]] {
  def context(): F[Contextual.Context]


  def withContextMap[T](f: Contextual.Context => Contextual.Context)
                       (body: => F[T]): F[T]

  def withContext[T](ctx: Contextual.Context)
                    (body: => F[T]): F[T] = withContextMap(_ => ctx)(body)


  def withContextAdd[T](ctx: Contextual.Context)
                       (body: => F[T]): F[T] = withContextMap(old => old ++ ctx)(body)

}

object Contextual {
  def apply[F[_] : Contextual]: Contextual[F] = implicitly[Contextual[F]]

  type Context = Map[String, String]

}


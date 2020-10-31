package a14e.commons.context

import a14e.commons.context.Contextual.Context
import a14e.commons.mdc.MdcEffect
import cats.{Applicative, Functor}
import cats.data.{ReaderT, StateT, Writer, WriterT}
import cats.effect.{CancelToken, ConcurrentEffect, ContextShift, Effect, ExitCase, Fiber, IO, Sync, SyncIO}
import com.typesafe.scalalogging.Logger
import org.slf4j.Marker

import scala.concurrent.ExecutionContext

trait Contextual[F[_]] {
  def context(): F[Contextual.Context]
}

object Contextual {
  def apply[F[_] : Contextual]: Contextual[F] = implicitly[Contextual[F]]

  type Context = Map[String, String]

  def readerT[INNER[_] : Applicative, CTX](read: CTX => Context): Contextual[ReaderT[INNER, CTX, *]] = {
    () => ReaderT.ask[INNER, CTX].map(read)
  }

  def stateT[INNER[_] : Applicative, CTX, F[_]](read: CTX => Context): Contextual[StateT[INNER, CTX, *]] = {
    () => StateT.get[INNER, CTX].map(read)
  }

  def mdcContext[F[_] : Sync]: Contextual[F] = {
    import cats.implicits._
    import scala.jdk.CollectionConverters._
    () => MdcEffect.getMdc[F]().map(x => x.asScala.to(Map))
  }

}

trait LazyContLogger[F[_]] {

  def logger[F[_] : Sync : Contextual] = new ContextualLogger[F](underlyingLogger)

  private lazy val underlyingLogger = Logger[this.type]
}



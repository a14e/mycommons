package a14e.commons.context

import cats.MonadError
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import org.slf4j.{MDC, Marker}
import cats.implicits._


class ContextualLogger[F[_] : Sync : Contextual](val underlying: Logger) {
  // Error

  def error(message: => String): F[Unit] = wrapToContext {
    underlying.error(message)
  }

  def error(message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.error(message, cause)
  }

  def error(marker: Marker, message: => String): F[Unit] = wrapToContext {
    underlying.error(marker, message)
  }

  def error(marker: Marker, message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.error(marker, message, cause)
  }


  // Warn

  def warn(message: => String): F[Unit] = wrapToContext {
    underlying.warn(message)
  }

  def warn(message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.warn(message, cause)
  }

  def warn(message: => String, args: Any*): F[Unit] = wrapToContext {
    underlying.warn(message, args)
  }

  def warn(marker: Marker, message: => String): F[Unit] = wrapToContext {
    underlying.warn(marker, message)
  }

  def warn(marker: Marker, message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.warn(marker, message, cause)
  }

  def warn(marker: Marker, message: => String, args: Any*): F[Unit] = wrapToContext {
    underlying.warn(marker, message, args)
  }

  // Info

  def info(message: => String): F[Unit] = wrapToContext {
    underlying.info(message)
  }

  def info(message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.info(message, cause)
  }

  def info(marker: Marker, message: => String): F[Unit] = wrapToContext {
    underlying.info(marker, message)
  }

  def info(marker: Marker, message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.info(marker, message, cause)
  }


  // Debug

  def debug(message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.debug(message, cause)
  }

  def debug(marker: Marker, message: => String): F[Unit] = wrapToContext {
    underlying.debug(marker, message)
  }

  def debug(marker: Marker, message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.debug(marker, message, cause)
  }

  // Trace

  def trace(message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.trace(message, cause)
  }


  def trace(marker: Marker, message: => String): F[Unit] = wrapToContext {
    underlying.trace(marker, message)
  }

  def trace(marker: Marker, message: => String, cause: Throwable): F[Unit] = wrapToContext {
    underlying.trace(marker, message, cause)
  }


  private def wrapToContext(block: => Unit): F[Unit] = {
    import scala.jdk.CollectionConverters._
    for {
      ctx <- Contextual[F].context()
      _ = MDC.setContextMap(ctx.asJava)
      _ = block
      _ = MDC.clear()
    } yield ()
  }
}

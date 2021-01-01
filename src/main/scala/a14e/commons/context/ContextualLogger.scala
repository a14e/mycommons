package a14e.commons.context

import cats.MonadError
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import org.slf4j.{MDC, Marker}
import cats.implicits._

import net.logstash.logback.marker.Markers._
import scala.jdk.CollectionConverters._

class ContextualLogger[F[_] : Sync : Contextual](val underlying: Logger) {
  // Error

  def error(message: => String, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.error(marker, message)
    }

  def error(message: => String, cause: Throwable, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.error(marker, message, cause)
    }


  // Warn

  def warn(message: => String, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.warn(marker, message)
    }

  def warn(message: => String, cause: Throwable, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.warn(marker, message, cause)
    }


  // Info

  def info(message: => String, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.info(marker, message)
    }

  def info(message: => String, cause: Throwable, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.info(marker, message, cause)
    }


  // Debug

  def debug(message: => String, cause: Throwable, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.debug(marker, message, cause)
    }

  def debug(message: => String, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.debug(marker, message)
    }


  // Trace

  def trace(message: => String, cause: Throwable, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.trace(marker, message, cause)
    }


  def trace(message: => String, kvs: (String, String)*): F[Unit] =
    makersWithMdc(kvs).map { marker =>
      underlying.trace(marker, message)
    }

  private def makersWithMdc(markers: Seq[(String, String)]): F[Marker] = {
    Contextual[F].context().map { ctx =>
      appendEntries((ctx ++ markers).asJava)
    }
  }
}

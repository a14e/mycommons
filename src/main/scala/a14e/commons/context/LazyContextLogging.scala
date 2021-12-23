package a14e.commons.context

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory


trait LazyContextLogging {

  def logger[F[_] : Sync : Contextual] = new ContextualLogger[F](underlyingLogger)

  private lazy val underlyingLogger = Logger(LoggerFactory.getLogger(getClass.getName))

}


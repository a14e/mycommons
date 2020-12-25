package a14e.commons.context

import cats.effect.Sync
import com.typesafe.scalalogging.Logger


trait LazyContextLogging {

  def logger[F[_] : Sync : Contextual] = new ContextualLogger[F](underlyingLogger)

  private lazy val underlyingLogger = Logger[this.type]
}


package a14e.commons.time

import java.time.{Duration, Instant}

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

trait InstantValueReader {

  implicit lazy val instantValueReader: ValueReader[Instant] = ValueReader[String].map(Instant.parse)
  implicit lazy val durationValueReader: ValueReader[Duration] = {
    (config: Config, path: String) => config.getDuration(path)
  }

}


object InstantValueReader extends InstantValueReader
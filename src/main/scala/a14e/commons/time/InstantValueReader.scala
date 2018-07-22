package a14e.commons.time

import java.time.Instant
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

trait InstantValueReader {

  implicit lazy val instantValueReader: ValueReader[Instant] = ValueReader[String].map(Instant.parse)

}


object InstantValueReader extends InstantValueReader
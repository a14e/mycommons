package a14e.utils.json

import java.time.Instant

import scala.language.implicitConversions

sealed trait LongInstant {

  def time: Instant
}

object LongInstant {

  implicit def apply(time: Instant): LongInstant = LongInstantImpl(time)

  implicit def toInstant(longInstant: LongInstant): Instant = {
    longInstant.asInstanceOf[LongInstantImpl].time
  }

  private case class LongInstantImpl(time: Instant) extends LongInstant
}

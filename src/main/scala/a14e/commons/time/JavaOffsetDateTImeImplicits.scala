package a14e.commons.time

import java.time.temporal.ChronoUnit
import java.time.{Duration, OffsetDateTime}

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions


trait JavaOffsetDateTImeImplicits {

  implicit def offsetDateTImeImplicitsToRich(i: OffsetDateTime): RichTimeOffsetDateTime = new RichTimeOffsetDateTime(i)
}

object JavaOffsetDateTImeImplicits extends JavaOffsetDateTImeImplicits

class RichTimeOffsetDateTime(val time: OffsetDateTime) extends AnyVal {
  import TimeImplicits._

  def isExpired: Boolean = time.isBefore(OffsetDateTime.now)


  def plus(duration: FiniteDuration): OffsetDateTime = time.plus(duration.toMillis, ChronoUnit.MILLIS)
  def minus(duration: FiniteDuration): OffsetDateTime = time.minus(duration.toMillis, ChronoUnit.MILLIS)


  def +(duration: FiniteDuration): OffsetDateTime = time.plus(duration)
  def -(duration: FiniteDuration): OffsetDateTime = time.minus(duration)

  def +(duration: Duration): OffsetDateTime = time.plus(duration)
  def -(duration: Duration): OffsetDateTime = time.minus(duration)


  def -(other: OffsetDateTime): Duration = Duration.ofMillis(time.toInstant.toEpochMilli - other.toInstant.toEpochMilli)

  def >(other: OffsetDateTime): Boolean = time.compareTo(other) > 0
  def >=(other: OffsetDateTime): Boolean = time.compareTo(other) >= 0
  def <(other: OffsetDateTime): Boolean = time.compareTo(other) < 0
  def <=(other: OffsetDateTime): Boolean = time.compareTo(other) <= 0
}

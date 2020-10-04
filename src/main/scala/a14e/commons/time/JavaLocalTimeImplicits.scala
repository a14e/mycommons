package a14e.commons.time

import java.time.{Duration, Instant, LocalTime, OffsetDateTime, ZoneOffset}

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions


trait JavaLocalTimeImplicits {

  implicit def localTimeToRich(i: LocalTime): RichLocalTime = new RichLocalTime(i)
}

object JavaLocalTimeImplicits extends JavaLocalTimeImplicits

class RichLocalTime(val time: LocalTime) extends AnyVal {
  import TimeImplicits._

  def isExpired: Boolean = time.isBefore(LocalTime.now)

  def +(duration: FiniteDuration): LocalTime = time.plusNanos(duration.toNanos)
  def -(duration: FiniteDuration): LocalTime = time.plusNanos(duration.toNanos)

  def +(duration: Duration): LocalTime = time.plus(duration)
  def -(duration: Duration): LocalTime = time.minus(duration)

  def -(other: LocalTime): Duration = Duration.ofNanos(time.toNanoOfDay - other.toNanoOfDay)

  def >(other: LocalTime): Boolean = time.compareTo(other) > 0
  def >=(other: LocalTime): Boolean = time.compareTo(other) >= 0
  def <(other: LocalTime): Boolean = time.compareTo(other) < 0
  def <=(other: LocalTime): Boolean = time.compareTo(other) <= 0

}

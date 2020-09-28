package a14e.commons.time

import java.time.{Duration, Instant}

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions


trait JavaInstantImplicits {

  implicit def instantToRich(i: Instant): RichTimeInstant = new RichTimeInstant(i)
}

object JavaInstantImplicits extends JavaInstantImplicits

class RichTimeInstant(val time: Instant) extends AnyVal {
  import TimeImplicits._

  def isExpired: Boolean = time.isBefore(Instant.now)

  def +(duration: FiniteDuration): Instant = time.plusMillis(duration.toMillis)
  def -(duration: FiniteDuration): Instant = time.plusMillis(duration.toMillis)

  def +(duration: Duration): Instant = time.plus(duration)
  def -(duration: Duration): Instant = time.minus(duration)

  def -(instant: Instant): Duration = Duration.ofMillis(time.toEpochMilli - instant.toEpochMilli)

  def >(other: Instant): Boolean = time.compareTo(other) > 0
  def >=(other: Instant): Boolean = time.compareTo(other) >= 0
  def <(other: Instant): Boolean = time.compareTo(other) < 0
  def <=(other: Instant): Boolean = time.compareTo(other) <= 0
}

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

  def isBetween(lower: Instant,
                upper: Instant,
                lowerClosed: Boolean = true,
                upperClosed: Boolean = false): Boolean = {

    val toTestTimeMillis = time.toEpochMilli
    val lowerMillis = lower.toEpochMilli
    val upperMillis = upper.toEpochMilli


    (lowerMillis < toTestTimeMillis || (lowerClosed && lowerMillis == toTestTimeMillis)) &&
    (upperMillis > toTestTimeMillis || (upperClosed && upperMillis == toTestTimeMillis))
  }


  def plus(duration: FiniteDuration): Instant = time.plusMillis(duration.toMillis)
  def minus(duration: FiniteDuration): Instant = time.minusMillis(duration.toMillis)


  def +(duration: FiniteDuration): Instant = time.plus(duration)
  def -(duration: FiniteDuration): Instant = time.minus(duration)

  def +(duration: Duration): Instant = time.plus(duration)
  def -(duration: Duration): Instant = time.minus(duration)


  def -(instant: Instant): Duration = Duration.ofMillis(time.toEpochMilli - instant.toEpochMilli)

  def >(other: Instant): Boolean = time.compareTo(other) > 0
  def >=(other: Instant): Boolean = time.compareTo(other) >= 0
  def <(other: Instant): Boolean = time.compareTo(other) < 0
  def <=(other: Instant): Boolean = time.compareTo(other) <= 0
}

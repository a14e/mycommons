package a14e.commons.time

import java.time.{Duration, Instant}
import java.time.temporal.ChronoUnit

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions


trait JavaDurationImplicits {

  implicit def toDurationBuilderLong(long: Long): JavaDurationBuilders = new JavaDurationBuilders(long)
  implicit def toDurationBuilderInt(int: Int): JavaDurationBuilders = toDurationBuilderLong(int)
  implicit def toDurationBuilderShort(short: Short): JavaDurationBuilders = toDurationBuilderLong(short)
  implicit def durationToRichJavaDuration(d: Duration): RichDuration = new RichDuration(d)

}

class JavaDurationBuilders(val timeValue: Long) extends AnyVal {

  def *(duration: Duration): Duration =  duration.multipliedBy(timeValue)

  def nanoseconds: Duration = Duration.ofNanos(timeValue)
  def nanos: Duration = nanoseconds
  def nanosecond: Duration = nanoseconds
  def nano: Duration = nanoseconds

  def microseconds: Duration = Duration.of(timeValue, ChronoUnit.MICROS)
  def micros: Duration = microseconds
  def microsecond: Duration = microseconds
  def micro: Duration = microseconds

  def milliseconds: Duration = Duration.ofMillis(timeValue)
  def millis: Duration = milliseconds
  def millisecond: Duration = milliseconds
  def milli: Duration = milliseconds

  def seconds: Duration = Duration.ofSeconds(timeValue)
  def second: Duration = seconds

  def minutes: Duration = Duration.ofMinutes(timeValue)
  def minute: Duration = minutes

  def hours: Duration = Duration.ofHours(timeValue)
  def hour: Duration = hours

  def days: Duration = Duration.ofDays(timeValue)
  def day: Duration = days
}


class RichDuration(val duration: Duration) extends AnyVal {
  def +(other: Duration): Duration = duration.plus(other)
  def -(other: Duration): Duration = duration.minus(other)

  def / (divisor: Long): Duration = duration.dividedBy(divisor)
  def * (multiplier: Long): Duration = duration.multipliedBy(multiplier)

  def >(other: Duration): Boolean = duration.compareTo(other) > 0
  def >=(other: Duration): Boolean = duration.compareTo(other) >= 0
  def <(other: Duration): Boolean = duration.compareTo(other) < 0
  def <=(other: Duration): Boolean = duration.compareTo(other) <= 0

  def asScala: FiniteDuration = TimeImplicits.javaDurationToConcurrentDuration(duration)
}


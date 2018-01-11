package a14e.utils.json

import java.time.Instant

import scala.concurrent.duration.Duration
import scala.language.implicitConversions

sealed trait Seconds[T] {

  def time: T
}

object Seconds {

  implicit def apply[T: TimeData](time: T): Seconds[T] = SecondsImpl[T](time)

  implicit def toTime[T: TimeData](longInstant: Seconds[T]): T = {
    longInstant.asInstanceOf[SecondsImpl[T]].time
  }

  private case class SecondsImpl[T](time: T) extends Seconds[T]

}

sealed trait Millis[T] {
  def time: T
}

object Millis {

  implicit def apply[T: TimeData](time: T): Millis[T] = MillisImpl(time)

  implicit def toTime[T: TimeData](longInstant: Millis[T]): T = {
    longInstant.asInstanceOf[MillisImpl[T]].time
  }

  private case class MillisImpl[T](time: T) extends Millis[T]

}

trait TimeData[T] {
  def toMillis(x: T): Long
  def toSeconds(x: T): Long


  def fromMillis(x: Long): T
  def fromSeconds(x: Long): T
}

object TimeData {
  implicit val InstantTime: TimeData[Instant] = new TimeData[Instant] {
    override def toMillis(x: Instant): Long = x.toEpochMilli

    override def toSeconds(x: Instant): Long = x.getEpochSecond

    override def fromMillis(x: Long): Instant = Instant.ofEpochMilli(x)

    override def fromSeconds(x: Long): Instant = Instant.ofEpochSecond(x)
  }

  implicit val DurationTime: TimeData[Duration] = new TimeData[Duration] {
    import scala.concurrent.duration._
    override def toMillis(x: Duration): Long = x.toMillis

    override def toSeconds(x: Duration): Long = x.toSeconds

    override def fromMillis(x: Long): Duration = x.millis

    override def fromSeconds(x: Long): Duration = x.seconds
  }
}
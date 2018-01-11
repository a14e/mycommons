package a14e.utils.json

import java.time.Instant

import shapeless.T

import scala.language.implicitConversions

sealed trait Seconds[T] {

  def time: T
}

object Seconds {

  implicit def apply[T: IsTime](time: T): Seconds[T] = SecondsImpl(time)

  implicit def toTime[T: IsTime](longInstant: Seconds[T]): T = {
    longInstant.asInstanceOf[SecondsImpl].time
  }

  private case class SecondsImpl[T](time: T) extends Seconds[T]

}

sealed trait Millis[T] {
  def time: T
}

object Millis {

  implicit def apply[T: IsTime](time: T): Millis[T] = MillisImpl(time)

  implicit def toTime[T: IsTime](longInstant: Millis[T]): T = {
    longInstant.asInstanceOf[MillisImpl].time
  }

  private case class MillisImpl[T](time: T) extends Millis[T]

}

sealed trait IsTime[T]

object IsTime {
  implicit val isInstant: IsTime[Instant] = new IsTime[Instant] {}
}
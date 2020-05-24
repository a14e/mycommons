package a14e.commons.encodings

import scala.concurrent.duration.FiniteDuration

import java.time.{Duration => JavaDuration, Instant, LocalDate, ZoneId}

object Nanos extends AsTag {
  type Nanos = this.type

  override type TO = Long

  implicit val FiniteDurationSecondsTaggedEncodings: TaggedEncodings[FiniteDuration, Long, Nanos] =
    new TaggedEncodings[FiniteDuration, Long, Nanos] {

      import scala.concurrent.duration._

      override def encode(x: FiniteDuration): Long = x.toNanos

      override def decode(x: Long): FiniteDuration = x.nanos
    }


  implicit val JavaDurationSecondsTaggedEncodings: TaggedEncodings[JavaDuration, Long, Nanos] =
    new TaggedEncodings[JavaDuration, Long, Nanos] {

      override def encode(x: JavaDuration): TO = x.toNanos

      override def decode(x: TO): JavaDuration = JavaDuration.ofNanos(x)
    }
}

package a14e.commons.encodings

import java.time.{Duration => JavaDuration, Instant, LocalDate, ZoneId}

import scala.concurrent.duration.FiniteDuration

object Seconds extends AsTag {
  type Seconds = this.type

  override type TO = Long

  implicit val InstantSecondsTaggedEncodings: TaggedEncodings[Instant, Long, Seconds] =
    new TaggedEncodings[Instant, Long, Seconds] {
      override def encode(x: Instant): Long = x.getEpochSecond

      override def decode(seconds: Long): Instant = Instant.ofEpochSecond(seconds)
    }

  implicit val LocalDateSecondsTaggedEncodings: TaggedEncodings[LocalDate, Long, Seconds] =
    new TaggedEncodings[LocalDate, Long, Seconds] {
      override def encode(x: LocalDate): Long = x.atStartOfDay(ZoneId.of("UTC")).toInstant.getEpochSecond

      override def decode(seconds: Long): LocalDate =
        Instant.ofEpochSecond(seconds).atZone(ZoneId.of("UTC")).toLocalDate
    }

  implicit val FiniteDurationSecondsTaggedEncodings: TaggedEncodings[FiniteDuration, Long, Seconds] =
    new TaggedEncodings[FiniteDuration, Long, Seconds] {

      import scala.concurrent.duration._

      override def encode(x: FiniteDuration): TO = x.toSeconds

      override def decode(x: TO): FiniteDuration = x.seconds
    }


  implicit val JavaDurationSecondsTaggedEncodings: TaggedEncodings[JavaDuration, Long, Seconds] =
    new TaggedEncodings[JavaDuration, Long, Seconds] {

      override def encode(x: JavaDuration): TO = x.getSeconds

      override def decode(x: TO): JavaDuration = JavaDuration.ofSeconds(x)
    }
}

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

// TODO NanosBigInt

object Millis extends AsTag {
  type Millis = this.type

  override type TO = Long

  implicit val InstantMillisTaggedEncodings: TaggedEncodings[Instant, Long, Millis] =
    new TaggedEncodings[Instant, Long, Millis] {
      override def encode(x: Instant): Long = x.toEpochMilli

      override def decode(millis: Long): Instant = Instant.ofEpochMilli(millis)
    }

  implicit val LocalDateMillisTaggedEncodings: TaggedEncodings[LocalDate, Long, Millis] =
    new TaggedEncodings[LocalDate, Long, Millis] {
      override def encode(x: LocalDate): Long = x.atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli

      override def decode(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate
    }

  implicit val FiniteDurationMillisTaggedEncodings: TaggedEncodings[FiniteDuration, Long, Millis] =
    new TaggedEncodings[FiniteDuration, Long, Millis] {

      import scala.concurrent.duration._

      override def encode(x: FiniteDuration): TO = x.toMillis

      override def decode(millis: TO): FiniteDuration = millis.milli
    }

  implicit val JavaDurationMillisTaggedEncodings: TaggedEncodings[JavaDuration, Long, Millis] =
    new TaggedEncodings[JavaDuration, Long, Millis] {

      override def encode(x: JavaDuration): TO = x.toMillis

      override def decode(x: TO): JavaDuration = JavaDuration.ofMillis(x)
    }
}

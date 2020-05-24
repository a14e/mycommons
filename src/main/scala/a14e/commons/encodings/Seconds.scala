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



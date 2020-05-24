package a14e.commons.encodings

import scala.concurrent.duration.FiniteDuration

import java.time.{Duration => JavaDuration, Instant, LocalDate, ZoneId}
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
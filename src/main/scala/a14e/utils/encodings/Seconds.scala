package a14e.utils.encodings

import java.time.{Instant, LocalDate, ZoneId}

import scala.concurrent.duration.Duration


object Seconds extends AsTag {
  type Seconds = this.type

  override type TO = Long

  implicit val InstantSeconds: TaggedEncodings[Instant, Long, Seconds] = new TaggedEncodings[Instant, Long, Seconds] {
    override def encode(x: Instant): Long = x.getEpochSecond

    override def decode(seconds: Long): Instant = Instant.ofEpochSecond(seconds)
  }

  implicit val LocalDateSeconds: TaggedEncodings[LocalDate, Long, Seconds] = new TaggedEncodings[LocalDate, Long, Seconds] {
    override def encode(x: LocalDate): Long = x.atStartOfDay(ZoneId.of("UTC")).toInstant.getEpochSecond

    override def decode(seconds: Long): LocalDate = Instant.ofEpochSecond(seconds).atZone(ZoneId.of("UTC")).toLocalDate
  }

  implicit val DurationSeconds:  TaggedEncodings[Duration, Long, Seconds]= new TaggedEncodings[Duration, Long, Seconds] {
    import scala.concurrent.duration._

    override def encode(x: Duration): TO = x.toSeconds

    override def decode(x: TO): Duration = x.seconds
  }

}



object Millis extends AsTag {
  type Millis = this.type

  override type TO = Long

  implicit val InstantMillis: TaggedEncodings[Instant, Long, Millis] = new TaggedEncodings[Instant, Long, Millis] {
    override def encode(x: Instant): Long = x.toEpochMilli

    override def decode(millis: Long): Instant = Instant.ofEpochMilli(millis)
  }

  implicit val LocalDateMillis: TaggedEncodings[LocalDate, Long, Millis] = new TaggedEncodings[LocalDate, Long, Millis] {
    override def encode(x: LocalDate): Long = x.atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli

    override def decode(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate
  }

  implicit val DurationMillis:  TaggedEncodings[Duration, Long, Millis]= new TaggedEncodings[Duration, Long, Millis] {
    import scala.concurrent.duration._

    override def encode(x: Duration): TO = x.toMillis

    override def decode(millis: TO): Duration = millis.milli
  }

}

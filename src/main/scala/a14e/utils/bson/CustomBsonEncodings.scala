package a14e.utils.bson

import java.time.Instant

import a14e.bson.decoder.BsonDecoder
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.encoder.BsonEncoder
import a14e.utils.json.{Millis, Seconds, TimeData}
import akka.util.ByteString

trait CustomBsonEncodings {
  implicit lazy val byteStringEncoder: BsonEncoder[ByteString] =
    implicitly[BsonEncoder[Array[Byte]]].contramap[ByteString](_.toArray)

  implicit lazy val byteStringDecoder: BsonDecoder[ByteString] =
    implicitly[BsonDecoder[Array[Byte]]].map(ByteString(_))


  implicit def MillisTimeEncoder[T: TimeData]: BsonEncoder[Millis[T]] =
    implicitly[BsonEncoder[Instant]].contramap[Millis[T]](x =>
      Instant.ofEpochMilli(implicitly[TimeData[T]].toMillis(x))
    )


  implicit def MillisTimeDecoder[T: TimeData]: BsonDecoder[Millis[T]] = {
    implicitly[BsonDecoder[Instant]].map[Millis[T]] { x =>
      implicitly[TimeData[T]].fromMillis(x.toEpochMilli)
    }
  }

  implicit def MillisSecondsEncoder[T: TimeData]: BsonEncoder[Seconds[T]] =
    implicitly[BsonEncoder[Instant]].contramap[Seconds[T]](x =>
      Instant.ofEpochSecond(implicitly[TimeData[T]].toSeconds(x))
    )


  implicit def MillisSecondsDecoder[T: TimeData]: BsonDecoder[Seconds[T]] = {
    implicitly[BsonDecoder[Instant]].map[Seconds[T]] { x =>
      implicitly[TimeData[T]].fromSeconds(x.getEpochSecond)
    }
  }
}

object CustomBsonEncodings extends CustomBsonEncodings




package a14e.utils.json

import java.time.Instant

import io.circe.{Decoder, Encoder}

trait TimeWrappersEncodings {

  implicit val MillisInstantEncoder: Encoder[Millis[Instant]] =
    Encoder.encodeLong.contramap[Millis[Instant]](_.time.toEpochMilli)


  implicit val MillisInstantDecoder: Decoder[Millis[Instant]] =
    Decoder.decodeLong.map[Millis[Instant]](x => Millis[Instant](Instant.ofEpochMilli(x)))


  implicit val SecondsInstantEncoder: Encoder[Seconds[Instant]] =
    Encoder.encodeLong.contramap[Seconds[Instant]](_.time.getEpochSecond)

  implicit val SecondsInstantDecoder: Decoder[Seconds[Instant]] =
    Decoder.decodeLong.map[Seconds[Instant]](x => Seconds[Instant](Instant.ofEpochSecond(x)))

}

object TimeWrappersEncodings extends TimeWrappersEncodings

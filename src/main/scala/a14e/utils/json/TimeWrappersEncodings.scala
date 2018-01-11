package a14e.utils.json

import java.time.Instant

import io.circe.{Decoder, Encoder}

trait TimeWrappersEncodings {

  implicit def MillisInstantEncoder[T: TimeData]: Encoder[Millis[T]] =
    Encoder.encodeLong.contramap[Millis[T]](x => implicitly[TimeData[T]].toMillis(x))


  implicit def MillisInstantDecoder[T: TimeData]: Decoder[Millis[T]] =
    Decoder.decodeLong.map[Millis[T]](x => implicitly[TimeData[T]].fromMillis(x))


  implicit def SecondsInstantEncoder[T: TimeData]: Encoder[Seconds[T]] =
    Encoder.encodeLong.contramap[Seconds[T]](x => implicitly[TimeData[T]].toSeconds(x))

  implicit def SecondsInstantDecoder[T: TimeData]: Decoder[Seconds[T]] =
    Decoder.decodeLong.map[Seconds[T]](x => implicitly[TimeData[T]].fromSeconds(x))

}

object TimeWrappersEncodings extends TimeWrappersEncodings

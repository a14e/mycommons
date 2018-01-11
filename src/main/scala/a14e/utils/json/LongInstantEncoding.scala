package a14e.utils.json

import java.time.Instant

import io.circe.{Decoder, Encoder}

trait LongInstantEncoding {

  implicit val LongInstantEncoder: Encoder[LongInstant] =
    Encoder.encodeLong.contramap[LongInstant](_.time.toEpochMilli)


  implicit val LongInstantDecoder: Decoder[LongInstant] =
    Decoder.decodeLong.map[LongInstant](x => LongInstant(Instant.ofEpochMilli(x)))

}

object LongInstantEncoding extends LongInstantEncoding

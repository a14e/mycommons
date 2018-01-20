package a14e.utils.json

import java.time.Instant

import a14e.utils.encodings.{AS, AsTag, TaggedDecoder, TaggedEncoder}
import io.circe.{Decoder, Encoder}

trait TaggedEncodings {

  implicit def taggedJsonEncoder[T, B <: AsTag](implicit
                                                encoder: TaggedEncoder[T, B#TO, B],
                                                toEncoder: Encoder[B#TO]): Encoder[AS[T, B]] =
    toEncoder.contramap[AS[T, B]](x => encoder.encode(x))


  implicit def taggedJsonDecoder[T, B <: AsTag](implicit
                                                decoder: TaggedDecoder[T, B#TO, B],
                                                fromEncoder: Decoder[B#TO]): Decoder[AS[T, B]] = {
    fromEncoder.map[AS[T, B]](x => decoder.decode(x))
  }


}

object TaggedEncodings extends TaggedEncodings

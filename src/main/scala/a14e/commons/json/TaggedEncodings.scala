package a14e.commons.json

import java.time.Instant

import a14e.commons.encodings.{AS, AsTag, TaggedDecoder, TaggedEncoder}
import play.api.libs.json.{Reads, Writes}
import play.api.libs.functional.syntax._

trait TaggedEncodings {

  implicit def taggedJsonEncoder[T, B <: AsTag](implicit
                                                encoder: TaggedEncoder[T, B#TO, B],
                                                toEncoder: Writes[B#TO]): Writes[AS[T, B]] =
    toEncoder.contramap[AS[T, B]](x => encoder.encode(x))


  implicit def taggedJsonDecoder[T, B <: AsTag](implicit
                                                decoder: TaggedDecoder[T, B#TO, B],
                                                fromEncoder: Reads[B#TO]): Reads[AS[T, B]] = {
    fromEncoder.map[AS[T, B]](x => decoder.decode(x))
  }


}

object TaggedEncodings extends TaggedEncodings

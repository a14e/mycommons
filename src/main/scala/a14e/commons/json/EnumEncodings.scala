package a14e.commons.json

import a14e.commons.enum.EnumFinder
import io.circe.{Decoder, Encoder}

trait EnumEncodings {
  implicit def enumerationValueEncoder[T <: Enumeration]: Encoder[T#Value] =
    Encoder[String].contramap[T#Value](_.toString)

  implicit def enumerationValueDecoder[ENUM <: Enumeration : EnumFinder]: Decoder[ENUM#Value] = {
    Decoder[String].map(implicitly[EnumFinder[ENUM]].find.withName(_))
  }
}

object EnumEncodings extends EnumEncodings

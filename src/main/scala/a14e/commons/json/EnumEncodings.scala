package a14e.commons.json

import a14e.commons.enum.EnumFinder
import io.circe.{Decoder, Encoder}

trait EnumEncodings {
  implicit def enumerationValueEncoder[ENUM <: Enumeration]: Encoder[ENUM#Value] = {
    Encoder[String].contramap[ENUM#Value](_.toString)
  }

  implicit def enumerationValueDecoder[ENUM <: Enumeration : EnumFinder]: Decoder[ENUM#Value] = {
    Decoder[String].map(EnumFinder[ENUM].find.withName(_))
  }
}

object EnumEncodings extends EnumEncodings

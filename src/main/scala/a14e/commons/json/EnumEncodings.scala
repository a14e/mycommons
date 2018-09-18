package a14e.commons.json

import a14e.commons.enum.EnumFinder
import play.api.libs.json.Writes
import play.api.libs.json.Reads
import play.api.libs.functional.syntax._

trait EnumEncodings {
  implicit def enumerationValueEncoder[T <: Enumeration]: Writes[T#Value] =
    implicitly[Writes[String]].contramap[T#Value](_.toString)

  implicit def enumerationValueDecoder[ENUM <: Enumeration : EnumFinder]: Reads[ENUM#Value] = {
    implicitly[Reads[String]].map(implicitly[EnumFinder[ENUM]].find.withName(_))
  }
}

object EnumEncodings extends EnumEncodings

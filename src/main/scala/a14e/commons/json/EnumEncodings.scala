package a14e.commons.json

import java.util.concurrent.atomic.AtomicReference

import akka.util.ByteString
import a14e.commons.enum.EnumFinder
import com.google.common.io.BaseEncoding
import com.typesafe.config.ConfigException.Generic
import io.circe._

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait EnumEncodings {
  implicit def enumerationValueEncoder[T <: Enumeration]: Encoder[T#Value] =
    Encoder[String].contramap[T#Value](_.toString)

  implicit def enumerationValueDecoder[ENUM <: Enumeration  : EnumFinder]: Decoder[ENUM#Value] = {
    Decoder[String].map(implicitly[EnumFinder[ENUM]].find.withName(_))
  }
}

object EnumEncodings extends EnumEncodings

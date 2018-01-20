package a14e.utils.json

import java.util.concurrent.atomic.AtomicReference

import akka.util.ByteString
import a14e.utils.enum.EnumFinder
import com.google.common.io.BaseEncoding
import com.typesafe.config.ConfigException.Generic
import io.circe._

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait EnumEncodings {
  implicit def enumerationValueEncoder[T <: Enumeration#Value]: Encoder[T] =
    Encoder[String].contramap[T](_.toString)

  implicit def enumerationValueDecoder[T <: Enumeration#Value : EnumFinder]: Decoder[T] = {
    Decoder[String].map(implicitly[EnumFinder[T]].find.withName(_).asInstanceOf[T])
  }

}

object EnumEncodings extends EnumEncodings

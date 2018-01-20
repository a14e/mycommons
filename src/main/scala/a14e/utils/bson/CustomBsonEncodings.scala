package a14e.utils.bson

import java.time.Instant

import a14e.bson.decoder.BsonDecoder
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.encoder.BsonEncoder
import a14e.utils.encodings.{AS, AsTag}
import a14e.utils.enum.EnumFinder
import akka.util.ByteString
import io.circe.{Decoder, Encoder}

trait CustomBsonEncodings {
  implicit lazy val byteStringEncoder: BsonEncoder[ByteString] =
    implicitly[BsonEncoder[Array[Byte]]].contramap[ByteString](_.toArray)

  implicit lazy val byteStringDecoder: BsonDecoder[ByteString] =
    implicitly[BsonDecoder[Array[Byte]]].map(ByteString(_))


  implicit def taggedBsonEncoder[T: BsonEncoder, B <: AsTag]: BsonEncoder[AS[T, B]] =
    implicitly[BsonEncoder[T]].contramap[AS[T, B]](x => x.value)


  implicit def taggedBsonDecoder[T: BsonDecoder, B <: AsTag]: BsonDecoder[AS[T, B]] = {
    implicitly[BsonDecoder[T]].map[AS[T, B]] { x => AS[T, B](x) }
  }


  implicit def enumerationValueEncoder[T <: Enumeration#Value]: BsonEncoder[T] =
    implicitly[BsonEncoder[String]].contramap[T](_.toString)

  implicit def enumerationValueDecoder[T <: Enumeration#Value : EnumFinder]: BsonDecoder[T] = {
    implicitly[BsonDecoder[String]].map(implicitly[EnumFinder[T]].find.withName(_).asInstanceOf[T])
  }
}

object CustomBsonEncodings extends CustomBsonEncodings




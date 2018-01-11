package a14e.utils.bson

import a14e.bson.decoder.BsonDecoder
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.decoder.BsonDecoder._
import a14e.bson.encoder.BsonEncoder
import akka.util.ByteString

trait CustomBsonEncodings {
  implicit lazy val byteStringEncoder: BsonEncoder[ByteString] =
    implicitly[BsonEncoder[Array[Byte]]].contramap[ByteString](_.toArray)

  implicit lazy val byteStringDecoder: BsonDecoder[ByteString] =
    implicitly[BsonDecoder[Array[Byte]]].map(ByteString(_))
}

object CustomBsonEncodings extends CustomBsonEncodings
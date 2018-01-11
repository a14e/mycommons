package a14e.utils.json

import akka.util.ByteString
import com.google.common.io.BaseEncoding
import io.circe.{Decoder, Encoder}


trait ByteStringEncoding {
  implicit val byteStringEncoder: Encoder[ByteString] =
    Encoder.encodeString
      .contramap[Array[Byte]](BaseEncoding.base64().encode)
      .contramap[ByteString](_.toArray)

  implicit val byteStringDecoder: Decoder[ByteString] =
    Decoder.decodeString.map(BaseEncoding.base64().decode).map(ByteString(_))
  
}


object ByteStringEncoding extends ByteStringEncoding
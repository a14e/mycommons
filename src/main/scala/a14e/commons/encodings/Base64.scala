package a14e.commons.encodings

import akka.util.ByteString
import com.google.common.io.BaseEncoding


object Base64 extends AsTag {
  type Base64 = this.type

  override type TO = String

  implicit val byteStingEncodings: TaggedEncodings[ByteString, String, Base64] =
    new TaggedEncodings[ByteString, String, Base64] {
      override def encode(x: ByteString): String = {
        BaseEncoding.base64().lowerCase().encode(x.toArray)
      }

      override def decode(base64: String): ByteString = {
        ByteString(BaseEncoding.base64().lowerCase().decode(base64))
      }
    }

  implicit val arrayEncodings: TaggedEncodings[Array[Byte], String, Base64] =
    new TaggedEncodings[Array[Byte], String, Base64] {
      override def encode(x: Array[Byte]): String = BaseEncoding.base64().lowerCase().encode(x)

      override def decode(base64: String): Array[Byte] = BaseEncoding.base64().lowerCase().decode(base64)
    }
}
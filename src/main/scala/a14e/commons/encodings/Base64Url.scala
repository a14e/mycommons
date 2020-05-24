package a14e.commons.encodings

import akka.util.ByteString
import com.google.common.io.BaseEncoding


object Base64Url extends AsTag {
  type Base64Url = this.type

  override type TO = String

  implicit val byteStingEncodings: TaggedEncodings[ByteString, String, Base64Url] =
    new TaggedEncodings[ByteString, String, Base64Url] {
      override def encode(x: ByteString): String = {
        BaseEncoding.base64Url().lowerCase().encode(x.toArray)
      }

      override def decode(base64: String): ByteString = {
        ByteString(BaseEncoding.base64Url().lowerCase().decode(base64))
      }
    }

  implicit val arrayEncodings: TaggedEncodings[Array[Byte], String, Base64Url] =
    new TaggedEncodings[Array[Byte], String, Base64Url] {
      override def encode(x: Array[Byte]): String = BaseEncoding.base64Url().lowerCase().encode(x)

      override def decode(base64: String): Array[Byte] = BaseEncoding.base64Url().lowerCase().decode(base64)
    }
}
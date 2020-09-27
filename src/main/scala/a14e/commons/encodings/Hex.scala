package a14e.commons.encodings

import com.google.common.io.BaseEncoding


object Hex extends AsTag {
  type Hex = this.type

  override type TO = String


  implicit val arrayEncodings: TaggedEncodings[Array[Byte], String, Hex] =
    new TaggedEncodings[Array[Byte], String, Hex] {
      override def encode(x: Array[Byte]): String = BaseEncoding.base16().lowerCase().encode(x)

      override def decode(base64: String): Array[Byte] = BaseEncoding.base16().lowerCase().decode(base64)
    }
}


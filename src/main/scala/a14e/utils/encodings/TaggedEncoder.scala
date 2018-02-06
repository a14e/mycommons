package a14e.utils.encodings

import java.time.Instant
import java.util.UUID

import a14e.utils.encodings.NumberEnum.NumberEnum
import a14e.utils.encodings.Seconds.{Seconds, TO}
import a14e.utils.enum.EnumFinder
import akka.util.ByteString
import com.google.common.io.BaseEncoding

import scala.concurrent.duration.Duration
import scala.language.implicitConversions


trait TaggedEncoder[FROM, TO, TAG] {

  def encode(x: FROM): TO
}


trait TaggedDecoder[TO, FROM, TAG] {

  def decode(x: FROM): TO
}

trait TaggedEncodings[FROM, TO, TAG] extends TaggedEncoder[FROM, TO, TAG] with TaggedDecoder[FROM, TO, TAG]


trait AsTag {
  type TO
}


trait AS[FROM, TAG <: AsTag] {
  def value: FROM
}

object AsImplicits {

  implicit class RichTaggedEncodings[T](val x: T) extends AnyVal {
    def as[B <: AsTag](implicit encoder: TaggedEncoder[T, B#TO, B]): B#TO = encoder.encode(x)

    def from[B <: AsTag](implicit decoder: TaggedDecoder[B#TO, T, B]): B#TO = decoder.decode(x)
  }

}


object AS {
  implicit def apply[FROM, TAG <: AsTag](from: FROM): AS[FROM, TAG] = AsImpl(from)

  implicit def from[T](as: AS[T, _]): T = as.value


  private case class AsImpl[FROM, TAG <: AsTag](value: FROM) extends AS[FROM, TAG] {
    override def toString: String = value.toString
  }

}


object NumberEnum extends AsTag {
  type NumberEnum = this.type

  override type TO = Int

  implicit def numberEnumEncodings[T <: Enumeration#Value : EnumFinder]: TaggedEncodings[T, Int, NumberEnum] =
    new TaggedEncodings[T, Int, NumberEnum] {
      override def encode(x: T): Int = x.id

      override def decode(id: Int): T = implicitly[EnumFinder[T]].find(id).asInstanceOf[T]
    }
}

object Base64 extends AsTag {
  type Base64 = this.type

  override type TO = String

  implicit val byteStingEncodings: TaggedEncodings[ByteString, String, Base64] =
    new TaggedEncodings[ByteString, String, Base64] {
      override def encode(x: ByteString): String = BaseEncoding.base64().encode(x.toArray)

      override def decode(base64: String): ByteString = ByteString(BaseEncoding.base64().decode(base64))
    }

  implicit val arrayEncodings: TaggedEncodings[Array[Byte], String, Base64] =
    new TaggedEncodings[Array[Byte], String, Base64] {
      override def encode(x: Array[Byte]): String = BaseEncoding.base64().encode(x)

      override def decode(base64: String): Array[Byte] = BaseEncoding.base64().decode(base64)
    }
}

object Base64Url extends AsTag {
  type Base64Url = this.type

  override type TO = String

  implicit val byteStingEncodings: TaggedEncodings[ByteString, String, Base64Url] =
    new TaggedEncodings[ByteString, String, Base64Url] {
      override def encode(x: ByteString): String = BaseEncoding.base64Url().encode(x.toArray)

      override def decode(base64: String): ByteString = ByteString(BaseEncoding.base64Url().decode(base64))
    }

  implicit val arrayEncodings: TaggedEncodings[Array[Byte], String, Base64Url] =
    new TaggedEncodings[Array[Byte], String, Base64Url] {
      override def encode(x: Array[Byte]): String = BaseEncoding.base64Url().encode(x)

      override def decode(base64: String): Array[Byte] = BaseEncoding.base64Url().decode(base64)
    }
}

object Hex extends AsTag {
  type Hex = this.type

  override type TO = String

  implicit val byteStingEncodings: TaggedEncodings[ByteString, String, Hex] =
    new TaggedEncodings[ByteString, String, Hex] {
      override def encode(x: ByteString): String = BaseEncoding.base16().encode(x.toArray)

      override def decode(base64: String): ByteString = ByteString(BaseEncoding.base16().decode(base64))
    }

  implicit val arrayEncodings: TaggedEncodings[Array[Byte], String, Hex] =
    new TaggedEncodings[Array[Byte], String, Hex] {
      override def encode(x: Array[Byte]): String = BaseEncoding.base16().encode(x)

      override def decode(base64: String): Array[Byte] = BaseEncoding.base16().decode(base64)
    }
}


object StringValue extends AsTag {
  type StringValue = this.type

  override type TO = String


  implicit val StringedInt: TaggedEncodings[Int, String, StringValue] =
    new TaggedEncodings[Int, String, StringValue] {
      override def decode(x: String): Int = x.toInt

      override def encode(x: Int): String = x.toString
    }

  implicit val StringedLong: TaggedEncodings[Long, String, StringValue] =
    new TaggedEncodings[Long, String, StringValue] {
      override def decode(x: String): Long = x.toLong

      override def encode(x: Long): String = x.toString
    }


  implicit val StringedDouble: TaggedEncodings[Double, String, StringValue] =
    new TaggedEncodings[Double, String, StringValue] {
      override def decode(x: String): Double = x.toDouble

      override def encode(x: Double): String = x.toString
    }

  implicit val StringedBoolean: TaggedEncodings[Boolean, String, StringValue] =
    new TaggedEncodings[Boolean, String, StringValue] {
      override def decode(x: String): Boolean = x.toBoolean

      override def encode(x: Boolean): String = x.toString
    }


  implicit val StringedBigInt: TaggedEncodings[BigInt, String, StringValue] =
    new TaggedEncodings[BigInt, String, StringValue] {
      override def decode(x: String): BigInt = BigInt(x)

      override def encode(x: BigInt): String = x.toString
    }


  implicit val StringedBigDecimal: TaggedEncodings[BigDecimal, String, StringValue] =
    new TaggedEncodings[BigDecimal, String, StringValue] {
      override def decode(x: String): BigDecimal = BigDecimal(x)

      override def encode(x: BigDecimal): String = x.toString
    }

  implicit val StringedUUID: TaggedEncodings[UUID, String, StringValue] =
    new TaggedEncodings[UUID, String, StringValue] {
      override def decode(x: String): UUID = UUID.fromString(x)

      override def encode(x: UUID): String = x.toString
    }
}



object UpperFromLower extends AsTag {
  type UpperFromLower = this.type

  override type TO = String

  type LOWER = String
  type UPPER = String

  implicit val UpperFromLowerEncodings: TaggedEncodings[UPPER, LOWER, UpperFromLower] =
    new TaggedEncodings[UPPER, LOWER, UpperFromLower] {
      override def decode(x: LOWER): UPPER = x.toUpperCase

      override def encode(x: UPPER): LOWER = x.toLowerCase
    }
}

object LowerFromUpper extends AsTag {
  type UpperFromLower = this.type

  override type TO = String

  type LOWER = String
  type UPPER = String

  implicit val UpperFromLowerEncodings: TaggedEncodings[LOWER, UPPER, UpperFromLower] =
    new TaggedEncodings[LOWER, UPPER, UpperFromLower] {
      override def decode(x: UPPER): LOWER = x.toLowerCase

      override def encode(x: LOWER): UPPER = x.toUpperCase
    }
}
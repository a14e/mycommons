package a14e.commons.encodings

import java.time.Instant
import java.util.UUID

import a14e.commons.encodings.NumberEnum.NumberEnum
import a14e.commons.encodings.Seconds.{Seconds, TO}
import a14e.commons.enum.EnumFinder
import com.google.common.io.BaseEncoding

import scala.concurrent.duration.Duration
import scala.language.implicitConversions


trait TaggedEncoder[FROM, TO, TAG] {

  def encode(x: FROM): TO
}

trait TaggedDecoder[TO, FROM, TAG] {

  def decode(x: FROM): TO
}

trait TaggedEncodings[FROM, TO, TAG]
  extends TaggedEncoder[FROM, TO, TAG]
    with TaggedDecoder[FROM, TO, TAG]


trait AsTag {
  type TO
}


trait AS[FROM, TAG <: AsTag] {
  def value: FROM
}



object AS {
  def from[FROM, TAG <: AsTag](from: FROM): AS[FROM, TAG] = AsImpl(from)

  def value[T](as: AS[T, _]): T = as.value

  object implicits {
    implicit def toAsTag[FROM, TAG <: AsTag](from: FROM): AS[FROM, TAG] = AsImpl(from)

    implicit def fromAsTag[T](as: AS[T, _]): T = as.value
  }

  private case class AsImpl[FROM, TAG <: AsTag](value: FROM) extends AS[FROM, TAG] {
    override def toString: String = value.toString
  }

}


object NumberEnum extends AsTag {
  type NumberEnum = this.type

  override type TO = Int

  implicit def numberEnumEncodings[ENUM <: Enumeration: EnumFinder]: TaggedEncodings[ENUM#Value, Int, NumberEnum] =
    new TaggedEncodings[ENUM#Value, Int, NumberEnum] {
      override def encode(x: ENUM#Value): Int = x.id

      override def decode(id: Int): ENUM#Value = EnumFinder[ENUM].find(id)
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
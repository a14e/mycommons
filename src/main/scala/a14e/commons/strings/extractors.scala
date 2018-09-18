package a14e.commons.strings

import java.time.Instant

import play.api.libs.json.{JsValue, Reads}

import scala.util.Try


object InstantString {
  def unapply(str: String): Option[Instant] = Try(Instant.parse(str)).toOption
}

object LongString {
  def unapply(str: String): Option[Long] = Try(str.toLong).toOption
}

object IntString {
  def unapply(str: String): Option[Int] = Try(str.toInt).toOption
}

object DoubleString {
  def unapply(str: String): Option[Double] = Try(str.toDouble).toOption
}

object ValidBooleanString {
  def unapply(str: String): Option[Boolean] = str match {
    case "true" => Some(true)
    case "false" => Some(false)
    case _ => None
  }
}

object InstantMillisString {
  def unapply(str: String): Option[Instant] = str match {
    case LongString(millis) =>
      val time = Instant.ofEpochMilli(millis)
      Some(time)
    case _ => None
  }
}


object InstantSecondsString {
  def unapply(str: String): Option[Instant] = str match {
    case LongString(millis) =>
      val time = Instant.ofEpochSecond(millis)
      Some(time)
    case _ => None
  }
}


class JsonExtractor[T: Reads] {
  def unapply(json: JsValue): Option[T] = json.asOpt[T]
}


class EnumExtractor[Enum <: Enumeration](enum: Enum) {
  def unapply(str: String): Option[Enum#Value] = Try(enum.withName(str)).toOption
}

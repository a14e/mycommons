package a14e.utils.strings

import java.time.Instant

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
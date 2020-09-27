package a14e.commons.enum

import scala.util.Try

trait ExtractableEnum {
  this: Enumeration =>

  def unapply(string: String): Option[Value] = Try(withName(string)).toOption
  def unapply(index: Int): Option[Value] = Try(apply(index)).toOption

}



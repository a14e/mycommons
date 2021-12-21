package a14e.commons.enum

import scala.util.Try

trait ExtractableEnum {
  this: Enumeration =>

  def unapply(string: String): Option[Value] = {
    values.byName.get(string)
  }
  def unapply(index: Int): Option[Value] = {
    Try(apply(index)).toOption
  }

}



package a14e.commons.enum

import scala.util.Try

trait ExtractableEnum {
  this: Enumeration =>

  def unapply(string: String): Option[Value] = {
    // неэффективно только на ошибках
    // так то это самый быстрый способ работы на successful case
    Try(this.withName(string)).toOption
  }

  def unapply(index: Int): Option[Value] = {
    Try(apply(index)).toOption
  }

}



package a14e.utils.strings

object StringImplicits {

  implicit class RichString(val str: String) extends AnyVal {
    def validateLength(minLen: Int,
                       maxLen: Int,
                       name: String)(implicit failAction: String => Unit): Unit = {
      val len = str.length
      val isValidLen = minLen <= len && len < maxLen
      if (!isValidLen)
        failAction(name)
    }
  }

  implicit class RichStringOption(val stringOption: Option.type) extends AnyVal {
    def trim(x: String): Option[String] = Option(x).map(_.trim).filter(x => !x.isEmpty)
  }

}

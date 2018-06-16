package a14e.utils.strings

object StringImplicits {

  implicit class RichString(val string: String) extends AnyVal {
    def validateLength(minLen: Int,
                       maxLen: Int,
                       name: String)(implicit failAction: String => Unit): Unit = {
      val len = string.length
      val isValidLen = minLen <= len && len < maxLen
      if (!isValidLen)
        failAction(name)
    }

    def removeSuffix(suffix: String): String = {
      if (string.endsWith(suffix)) string.dropRight(suffix.length)
      else string
    }

    def removePrefix(prefix: String): String = {
      if (string.startsWith(prefix)) string.drop(prefix.length)
      else string
    }
  }

  implicit class RichStringOption(val stringOption: Option.type) extends AnyVal {
    def trim(x: String): Option[String] = Option(x).map(_.trim).filter(x => !x.isEmpty)
  }

}

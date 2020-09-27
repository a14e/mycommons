package a14e.commons.strings

object StringImplicits {

  implicit class RichStringOption(val stringOption: Option.type) extends AnyVal {
    def trim(x: String): Option[String] = Option(x).map(_.trim).filter(x => !x.isEmpty)
  }

}

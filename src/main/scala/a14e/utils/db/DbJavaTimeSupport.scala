package a14e.utils.db

import java.time.Instant
import java.util.Date

trait DbJavaTimeSupport {
  this: AsyncContext[_, _, _] =>

  implicit lazy val encodeInstant: MappedEncoding[Instant, java.util.Date] =
    MappedEncoding[Instant, java.util.Date] { (x: Instant) =>
      Date.from(x)
    }
  implicit lazy val decodeInstant: MappedEncoding[java.util.Date, Instant] =
    MappedEncoding[java.util.Date, Instant] { date =>
      date.toInstant
    }

  implicit class InstantQuotes(left: Instant) {
    def >(right: Instant) = quote(infix"$left > $right".as[Boolean])

    def <(right: Instant) = quote(infix"$left < $right".as[Boolean])

    def >=(right: Instant) = quote(infix"$left >= $right".as[Boolean])

    def <=(right: Instant) = quote(infix"$left <= $right".as[Boolean])
  }

  implicit class OptionalQuotes[T](left: T) {

    def optFitlering(fOpt: Option[T => Quoted[Boolean]]): Quoted[Boolean] = {
      fOpt match {
        case None => quote(true)
        case Some(f) => quote(f(left))
      }
    }
  }


}


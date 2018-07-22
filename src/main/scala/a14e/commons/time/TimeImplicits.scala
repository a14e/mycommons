package a14e.commons.time

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

object TimeImplicits {


  implicit class RichTime(val time: Instant) extends AnyVal {

    def isExpired: Boolean = time.isBefore(Instant.now)

    def isBetween(lower: Instant,
                  upper: Instant,
                  lowerClosed: Boolean = false,
                  upperClosed: Boolean = true): Boolean = {

      val toTestTimeMillis = time.toEpochMilli
      val lowerMillis = lower.toEpochMilli
      val upperMillis = upper.toEpochMilli


      (lowerMillis < toTestTimeMillis || (lowerClosed && lowerMillis == toTestTimeMillis)) &&
      (upperMillis > toTestTimeMillis || (upperClosed && upperMillis == toTestTimeMillis))

    }


    def plus(duration: FiniteDuration): Instant = time.plusMillis(duration.toMillis)

    def minus(duration: FiniteDuration): Instant = time.minusMillis(duration.toMillis)

  }
}

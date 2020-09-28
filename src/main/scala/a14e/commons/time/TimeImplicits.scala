package a14e.commons.time

import java.time.{Duration, Instant}

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.language.implicitConversions

trait TimeImplicits
  extends JavaDurationImplicits
    with JavaToScalaDurationConverters
    with JavaInstantImplicits
    with JavaOffsetDateTImeImplicits

object TimeImplicits extends TimeImplicits {

  /** TODO имплиситы чтобы делать 2 * 2.seconds */
}


trait JavaToScalaDurationConverters {

  implicit def javaDurationToConcurrentDuration(d: Duration): FiniteDuration = {
    new DurationLong(d.toNanos).nanos
  }

}

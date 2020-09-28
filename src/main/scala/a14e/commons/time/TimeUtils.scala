package a14e.commons.time

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}

import a14e.commons.encodings.AsTag
import a14e.commons.strings.LongString

import scala.collection.mutable


object TimeUtils {

  final val ScalaInf: scala.concurrent.duration.Duration = scala.concurrent.duration.Duration.Inf
  final val Inf = Duration.ofMillis(Long.MaxValue)
}
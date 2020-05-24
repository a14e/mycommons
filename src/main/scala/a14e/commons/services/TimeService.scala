package a14e.commons.services

import java.time.{Clock, Instant}


trait TimeService {
  def now: Instant
  def clock: Clock
}

final class TimeServiceImpl extends TimeService {
  override def now: Instant = Instant.now()
  override def clock: Clock = Clock.systemUTC()
}


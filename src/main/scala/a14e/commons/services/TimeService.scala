package a14e.commons.services

import java.time.Instant


trait TimeService {
  def now: Instant
}

final class TimeServiceImpl extends TimeService {
  override def now: Instant = Instant.now()
}


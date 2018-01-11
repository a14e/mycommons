package a14e.utils.services

import java.time.Instant


trait TimeService {
  def now: Instant
}

class TimeServiceImpl extends TimeService {
  override def now: Instant = Instant.now()
}


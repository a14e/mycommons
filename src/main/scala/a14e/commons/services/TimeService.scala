package a14e.commons.services

import java.time.{Clock, Instant}

import cats.effect.Sync


trait TimeService[F[_]] {
  def now: F[Instant]
  def clock: F[Clock]
}

final class TimeServiceImpl[F[_]: Sync] extends TimeService[F] {
  protected val F = Sync[F]
  override def now: F[Instant] = F.delay(Instant.now())
  override def clock: F[Clock] = F.delay(Clock.systemUTC())
}


package a14e.commons.mdc

import cats.effect.{Clock, Sync, Timer}
import cats.implicits._

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds


class MdcTimer[F[_]: Sync](context: Timer[F]) extends Timer[F] {
  override def clock: Clock[F] = context.clock

  override def sleep(duration: FiniteDuration): F[Unit] = {
    for {
      mdc <- MdcEffect.getMdc[F]()
      _ <- context.sleep(duration)
      _ <- MdcEffect.setMdc[F](mdc)
    } yield ()
  }
}

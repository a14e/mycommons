package a14e.commons.catseffect

import cats.effect._
import cats.implicits._
import scala.concurrent.ExecutionContext

class MdcContextShift[F[_]: Sync](context: ContextShift[F]) extends ContextShift[F] {

  override def shift: F[Unit] = {
    for {
      mdc <- MdcEffect.getMdc[F]()
      _ <- context.shift
      _ <- MdcEffect.setMdc[F](mdc)
    } yield ()
  }

  override def evalOn[A](newContext: ExecutionContext)(block: F[A]): F[A] = {

    Sync[F].bracket(MdcEffect.getMdc[F]()) { mdc =>
      context.evalOn(newContext) {
        /** тут установим mdc, потом все вернем */
        MdcEffect.withMdc(mdc)(block)
      }
    }(mdc => MdcEffect.setMdc(mdc)) // когда мы вернулись mdc должен остаться
  }

}

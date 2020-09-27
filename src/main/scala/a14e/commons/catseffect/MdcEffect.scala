package a14e.robobpm.utils.catseffect


import cats.effect._
import cats.implicits._
import org.slf4j.MDC

import scala.language.higherKinds

object MdcEffect {
  type MdcMap = java.util.Map[String, String]

  def getMdc[F[_] : Sync](): F[MdcMap] = Sync[F].delay(MDC.getCopyOfContextMap)

  def setMdc[F[_] : Sync](mdc: MdcMap): F[Unit] = {
    if (mdc eq null) Sync[F].unit
    else Sync[F].delay(MDC.setContextMap(mdc))
  }

  def putKey[F[_] : Sync](key: String, value: String): F[Unit] = {
    Sync[F].delay(MDC.put(key, value))
  }

  def withMdc[F[_] : Sync, T](mdc: MdcMap)
                             (task: => F[T]): F[T] = {
    Sync[F].bracket(getMdc()) { _ =>
      setMdc(mdc) *> task
    }(setMdc(_))
  }
}
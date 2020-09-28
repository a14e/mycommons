package a14e.commons.mdc

import cats.effect.Sync
import org.apache.commons.codec.binary.Hex

import scala.util.Random

object ContextEffect {
  def addContext[F[_] : Sync](): F[Unit] = Sync[F].suspend {
    MdcEffect.putKey("traceId", generateTraceId())
  }

  private def generateTraceId(): String = {
    val bytes = Random.nextBytes(8)
    Hex.encodeHexString(bytes)
  }
}

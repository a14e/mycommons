package a14e.commons.json

import a14e.commons.enum.EnumFinder
import io.circe.{Decoder, Encoder, Json}

trait UnitEncodings {
  implicit val unitEncoder: Encoder[Unit] = Encoder[Unit](_ => Json.obj())
  implicit val unitDecoder: Decoder[Unit] = Decoder.const((): Unit)
}


object UnitEncodings extends UnitEncodings
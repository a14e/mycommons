package a14e.commons.json

import a14e.commons.enum.EnumFinder
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsString, Reads, Writes}

trait UnitEncodings {
  implicit val unitEncoder: Writes[Unit] = Writes[Unit](_ => JsString("OK"))


  implicit val unitDeconder: Writes[Unit] = Writes[Unit](_ => JsString("OK"))
}


object UnitEncodings extends UnitEncodings
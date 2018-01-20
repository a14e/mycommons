package a14e.utils.json

import io.circe.{Encoder, Json}
import io.circe.syntax._

import scala.language.implicitConversions

trait CustomJsonEncodings
  extends ByteStringEncoding
    with EnumEncodings
    with IdEncoding
    with TaggedEncodings

object CustomJsonEncodings extends CustomJsonEncodings


object JsBuild {

  sealed trait JsWrapper {
    def json: Json
  }

  private case class JsWrapperImpl(json: Json) extends JsWrapper

  implicit def jsonToJsWrapper[T: Encoder](x: T): JsWrapper = JsWrapperImpl(x.asJson)

  def obj(fields: (String, JsWrapper)*): Json = {
    val jsonFieldsSeq = fields.toStream.map { case (k, v) => k -> v.json }
    Json.fromFields(jsonFieldsSeq)
  }

  def arr(elems: JsWrapper*): Json = {
    val jsonElems = elems.toStream.map(_.json)
    Json.fromValues(jsonElems)
  }
}
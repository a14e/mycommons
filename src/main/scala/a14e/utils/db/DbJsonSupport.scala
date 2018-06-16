package a14e.utils.db

import io.circe.{parser, Decoder => CirceDecoder, Encoder => CirceEncoder}
import io.circe.syntax._
import io.getquill.context.async.AsyncContext

trait DbJsonSupport {
  this: AsyncContext[_, _, _] =>

  def jsonEncoder[T: CirceEncoder]: MappedEncoding[T, String] =
    MappedEncoding[T, String] { (x: T) =>
      x.asJson.noSpaces
    }

  def jsonDecoder[T: CirceDecoder]: MappedEncoding[String, T] =
    MappedEncoding[String, T] { (x: String) =>
      parser.parse(x).flatMap(_.as[T]).toTry.get
    }


}


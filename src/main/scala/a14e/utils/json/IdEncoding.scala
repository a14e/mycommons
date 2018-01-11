package a14e.utils.json

import a14e.bson.ID
import io.circe.{Decoder, Encoder}

trait IdEncoding {


  implicit def idEncoder[T](implicit encoder: Encoder[T]): Encoder[ID[T]] = encoder.contramap(_.value)
  implicit def idDecoder[T](implicit decoder: Decoder[T]): Decoder[ID[T]] = decoder.map(ID(_))
}
object IdEncoding extends IdEncoding
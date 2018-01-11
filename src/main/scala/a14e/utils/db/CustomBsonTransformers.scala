package a14e.utils.db

import java.time.Instant
import java.util.Date
import org.mongodb.scala.bson.BsonTransformer

trait CustomBsonTransformers {

  implicit def enumToBsonTransformer[T <: Enumeration]: BsonTransformer[T#Value] = {
    implicitly[BsonTransformer[String]].contramap[T#Value](_.toString)
  }


  implicit val instantTransformer: BsonTransformer[Instant] = {
    implicitly[BsonTransformer[Date]].contramap[Instant](i => new Date(i.getEpochSecond))
  }

  implicit class RichBsonTransformer[T](transformer: BsonTransformer[T]) {
    def contramap[B](f: B => T): BsonTransformer[B] = (value: B) => transformer(f(value))
  }
}


object CustomBsonTransformers extends CustomBsonTransformers {

}
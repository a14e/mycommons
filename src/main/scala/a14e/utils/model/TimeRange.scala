package a14e.utils.model

import java.time.Instant

import io.swagger.annotations.ApiModelProperty
import org.mongodb.scala.bson.collection.immutable.Document
import a14e.utils.db.CustomBsonTransformers._

case class TimeRange(@ApiModelProperty(value = "Начало интервала времени", required = false, dataType = "dateTime")from: Option[Instant] = None,
                     @ApiModelProperty(value = "Конец интервала времени", required = false, dataType = "dateTime")till: Option[Instant] = None)

object TimeRange {
  def empty: TimeRange = TimeRange(None, None)

  def toBson(fieldName: String,
             range: TimeRange): Document = {
    val fromBson = range.from.fold(Document()) { from =>
      Document(fieldName -> Document("$gte" -> from))
    }

    val tillBson = range.till.fold(Document()) { from =>
      Document(fieldName -> Document("$lte" -> from))
    }

    fromBson ++ tillBson
  }

}






package a14e.utils.model

import a14e.utils.controller.Throwers.badRequest
import a14e.utils.model
import a14e.utils.model.SortOrder.SortOrder
import io.swagger.annotations.ApiModelProperty
import org.mongodb.scala.bson.collection.immutable.Document
// TODO убрать?
case class SortSettings(@ApiModelProperty(value = "Направление сортировки", allowableValues = "asc, desc", required = false, dataType = "string") direction: SortOrder,
                        @ApiModelProperty(value = "Ключ для сортировки", dataType = "string", required = false) key: String)


object SortSettings {

  def toBson(validKeys: Set[String],
             sort: SortSettings*): Document = {
    sort.map(singleSortBson(validKeys, _)).foldLeft(Document())(_ ++ _)
  }

  private def singleSortBson(validKeys: Set[String],
                             sort: SortSettings): Document = {
    val isValidKey = validKeys(sort.key)
    if (!isValidKey)
      badRequest(s"invalid sort key ${sort.key}")

    Document(sort.key.toString -> direction(sort.direction))
  }

  import SortOrder._

  private def direction(sortDirection: SortOrder): Int = sortDirection match {
    case Asc => 1
    case Desc => -1
    case _ => badRequest(s"unsupported sort direction $sortDirection")
  }

}


object SortOrder extends Enumeration {
  type SortOrder = Value
  val Asc: SortOrder = Value("asc")
  val Desc: SortOrder = Value("desc")
}

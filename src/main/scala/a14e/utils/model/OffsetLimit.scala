package a14e.utils.model

import io.swagger.annotations.ApiModelProperty

case class OffsetLimit(@ApiModelProperty(value = "Сдвиг",  required = false, example = "0")offset: Int = OffsetLimit.defaultOffset,
                       @ApiModelProperty(value = "Лимит", required = false, example = "20")limit: Int = OffsetLimit.defaultLimit)


object OffsetLimit {
  val defaultOffset = 0
  val defaultLimit = 20

  val Empty = OffsetLimit(defaultOffset, defaultLimit)
}
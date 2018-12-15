package a14e.commons.controller

import scala.util.control.NoStackTrace


object RoutesControlErrors {

  case class NotFound(text: String = "") extends RuntimeException(text)

  case class Unauthorized(text: String = "") extends RuntimeException(text)

  case class Forbidden(text: String = "") extends RuntimeException(text)

  case class BadRequest(text: String = "") extends RuntimeException(text)

  case class InternalServerError(text: String = "") extends RuntimeException(text)
}


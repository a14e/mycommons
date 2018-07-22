package a14e.commons.controller

import scala.util.control.NoStackTrace


object RoutesControlErrors {

  case class NotFound(text: String = "") extends NoStackTrace

  case class Unauthorized(text: String = "") extends NoStackTrace

  case class Forbidden(text: String = "") extends NoStackTrace

  case class BadRequest(text: String = "") extends NoStackTrace

  case class InternalServerError(text: String = "") extends NoStackTrace
}


package a14e.commons.controller

object Throwers {

  def failWith[T](err: Throwable): T = throw err

  def failWithNotFound[T](text: String = ""): T = throw RoutesControlErrors.NotFound(text: String)

  def failWithUnauthorized[T](text: String = ""): T = throw RoutesControlErrors.Unauthorized(text: String)

  def failWithForbidden[T](text: String = ""): T = throw RoutesControlErrors.Forbidden(text: String)

  def failWithBadRequest[T](text: String = ""): T = throw RoutesControlErrors.BadRequest(text: String)

  def failWithInternalServerError[T](text: String = ""): T = throw RoutesControlErrors.InternalServerError(text: String)
}


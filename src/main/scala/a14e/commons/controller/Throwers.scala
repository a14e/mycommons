package a14e.commons.controller

object Throwers {

  def notFound[T](text: String = ""): T = throw RoutesControlErrors.NotFound(text: String)

  def unauthorized[T](text: String = ""): T = throw RoutesControlErrors.Unauthorized(text: String)

  def forbidden[T](text: String = ""): T = throw RoutesControlErrors.Forbidden(text: String)

  def badRequest[T](text: String = ""): T = throw RoutesControlErrors.BadRequest(text: String)

  def internalServerError[T](text: String = ""): T = throw RoutesControlErrors.InternalServerError(text: String)
}


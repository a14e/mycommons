package a14e.utils.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, handleExceptions, pathPrefix, _}
import akka.http.scaladsl.server.{ExceptionHandler, Route, _}
import a14e.utils.configs.{ConfigurationModule, ServerConfiguration}
import a14e.utils.controller.{CustomAkkaDirectives, RoutesControlErrors}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import a14e.utils.controller.Throwers._

trait HttpModule {
  this: LazyLogging
    with ControllersModule
    with CustomAkkaDirectives
    with ServerConfiguration =>

  import RouteConcatenation._

  def routes(logger: Logger): Route = {
    val withRejectionHandling = controllers.map(_.route).reduce(_ ~ _)
    val withoutRejectionHandling = afterRejectControllers.map(_.route).reduce(_ ~ _)
    val apiControllers = pathPrefix("api" / "v1")(withRejectionHandling)

    handleExceptions(generateExceptionHandler) {
      logData(logger, enableLogging, strictJson = true) {
        (handleRejections(rejectionHandler) & setCors(enableCors)) {
          apiControllers
        }
      } ~
      withoutRejectionHandling
    }
  }

  private def generateExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case RoutesControlErrors.NotFound(text) => complete(StatusCodes.NotFound -> text)
      case RoutesControlErrors.Unauthorized(text) => complete(StatusCodes.Unauthorized -> text)
      case RoutesControlErrors.Forbidden(text) => complete(StatusCodes.Forbidden -> text)
      case RoutesControlErrors.BadRequest(text) => complete(StatusCodes.BadRequest -> text)
      case RoutesControlErrors.InternalServerError(text) => complete(StatusCodes.InternalServerError -> text)
      case other =>
        logger.warn("request completed with error =( ", other)
        complete(StatusCodes.InternalServerError -> other.getMessage)
    }

  private val rejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthorizationFailedRejection =>
        complete(StatusCodes.Unauthorized -> "you are not authorized")
    }.result()
}

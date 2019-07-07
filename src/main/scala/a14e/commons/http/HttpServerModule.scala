package a14e.commons.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, handleExceptions, pathPrefix, _}
import akka.http.scaladsl.server.{ExceptionHandler, Route, _}
import a14e.commons.configs.{ConfigurationModule, ServerConfiguration}
import a14e.commons.controller.{Controller, CustomAkkaDirectives, RoutesControlErrors}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import a14e.commons.controller.Throwers._

import scala.util.control.NonFatal

trait HttpServerModule {
  this: LazyLogging
    with ControllersModule
    with CustomAkkaDirectives
    with ServerConfiguration =>

  import RouteConcatenation._

  def routes(logger: Logger,
             versionString: String = "v1",
             controllers: Seq[Controller] = this.controllers,
             afterRejectControllers: Seq[Controller] = this.afterRejectControllers): Route = {
    val withRejectionHandling = controllers.foldLeft(reject: Route)(_ ~ _.route)
    val withoutRejectionHandling = afterRejectControllers.foldLeft(reject: Route)(_ ~ _.route)
    val apiControllers = pathPrefix("api" / versionString)(withRejectionHandling)

    logData(logger, enableLogging, strictJson = true) {
      handleExceptions(generateExceptionHandler(logger)) {
        (handleRejections(rejectionHandler)) {
          apiControllers
        }
      }
    } ~ withoutRejectionHandling
  }

  private def generateExceptionHandler(logger: Logger): ExceptionHandler =
    ExceptionHandler {
      case NonFatal(err) =>
        logger.warn("Response completed with error =(", err)
        err match {
          case RoutesControlErrors.NotFound(text) =>
            complete(StatusCodes.NotFound -> text)
          case RoutesControlErrors.Unauthorized(text) =>
            complete(StatusCodes.Unauthorized -> text)
          case RoutesControlErrors.Forbidden(text) =>
            complete(StatusCodes.Forbidden -> text)
          case RoutesControlErrors.BadRequest(text) =>
            complete(StatusCodes.BadRequest -> text)
          case RoutesControlErrors.InternalServerError(text) =>
            complete(StatusCodes.InternalServerError -> text)
          case _ =>
            complete(StatusCodes.InternalServerError -> "Server error. Try again latter")
        }
    }

  private val rejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthorizationFailedRejection =>
        complete(StatusCodes.Unauthorized -> "you are not authorized")
    }.result()
}

package a14e.commons.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, handleExceptions, pathPrefix, _}
import akka.http.scaladsl.server.{ExceptionHandler, Route, _}
import a14e.commons.configs.ConfigurationModule
import a14e.commons.controller.{Controller, CustomAkkaDirectives, RoutesControlErrors}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import a14e.commons.controller.Throwers._
import play.api.libs.json.Json

import scala.util.control.NonFatal

trait HttpServerModule {
  this: LazyLogging
    with CustomAkkaDirectives =>

  import RouteConcatenation._

  def routes(logger: Logger,
             versionString: String = "v1",
             controllers: Seq[Controller],
             afterRejectControllers: Seq[Controller]): Route = {
    val withRejectionHandling = controllers.foldLeft(reject: Route)(_ ~ _.route)
    val withoutRejectionHandling = afterRejectControllers.foldLeft(reject: Route)(_ ~ _.route)
    val apiControllers = pathPrefix("api" / versionString)(withRejectionHandling)

    logData(logger, strictJson = true) {
      handleExceptions(generateExceptionHandler(logger)) {
        (handleRejections(rejectionHandler)) {
          apiControllers
        }
      }
    } ~ withoutRejectionHandling
  }

  import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
  private def generateExceptionHandler(logger: Logger): ExceptionHandler =
    ExceptionHandler {
      case NonFatal(err) =>
        logger.warn("Response completed with error =(", err)
        err match {
          case RoutesControlErrors.NotFound(text) =>
            complete(StatusCodes.NotFound -> responseErr(text))
          case RoutesControlErrors.Unauthorized(text) =>
            complete(StatusCodes.Unauthorized -> responseErr(text))
          case RoutesControlErrors.Forbidden(text) =>
            complete(StatusCodes.Forbidden -> responseErr(text))
          case RoutesControlErrors.BadRequest(text) =>
            complete(StatusCodes.BadRequest -> responseErr(text))
          case RoutesControlErrors.InternalServerError(text) =>
            complete(StatusCodes.InternalServerError -> responseErr(text))
          case _ =>
            complete(StatusCodes.InternalServerError -> responseErr("Server error. Try again latter"))
        }
    }

  private def responseErr(text: String) = Json.obj("message" -> text)

  private val rejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthorizationFailedRejection =>
        complete(StatusCodes.Unauthorized -> "you are not authorized")
    }.result()
}

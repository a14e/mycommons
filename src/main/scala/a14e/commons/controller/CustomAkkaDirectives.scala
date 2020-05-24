package a14e.commons.controller

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RespondWithDirectives.respondWithHeaders
import akka.http.scaladsl.server.directives._
import akka.http.scaladsl.server.directives.BasicDirectives.{extractRequestContext, mapRouteResult}
import akka.stream.Materializer
import com.typesafe.scalalogging.{LazyLogging, Logger}

import scala.collection.immutable
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.implicitConversions

trait CustomAkkaDirectives {
  this: LazyLogging =>

  val pathBoolean: PathMatcher1[Boolean] = Segment.flatMap {
    case "true" => Some(true)
    case "false" => Some(false)
    case _ => None
  }

  def logData(logger: Logger,
              strictJson: Boolean,
              strictJsonTimeout: FiniteDuration = 200.seconds): Directive0 = {
    val strictJsonDirective = if (!strictJson) pass else correctAndToStrictIfJson(strictJsonTimeout)

    val loggingDirective = {
      val requestId = "request-" + UUID.randomUUID().toString
      debuggingRequestResponse(requestId, logger) &
        respondWithDefaultHeader(RawHeader("Request-Id", requestId))

    }

    strictJsonDirective & loggingDirective
  }


  def correctAndToStrictIfJson(strictJsonTimeout: FiniteDuration): Directive[Unit] =
    extractRequest.flatMap { request =>
      if (request.entity.contentType == ContentTypes.`application/json`) {
        toStrictEntity(strictJsonTimeout) &
          mapRequest { request =>
            // так как тип иногда бывает binary, надо подкорректировать
            val newEntity = request.entity.withContentType(ContentTypes.`application/json`)
            request.withEntity(newEntity)
          }

      } else pass
    }

  def debuggingRequestResponse(requestId: String,
                               logger: Logger): Directive0 = {
    extractRequestContext.flatMap { ctx ⇒

      logger.info(
        s"""
           |Request:
           |requestId = $requestId
           |request = ${ctx.request}""".stripMargin)

      mapRouteResult { result ⇒

        logger.info(
          s"""
             |Response:
             |requestId = $requestId
             |response = $result
        """.stripMargin)

        result
      }
    }
  }


  def userAgentHeader: Directive1[Option[`User-Agent`]] =
    HeaderDirectives.optionalHeaderValueByType[`User-Agent`]((): Unit)

  private val allowedMethods = immutable.Seq(DELETE, GET, OPTIONS, PATCH, POST, PUT)
}




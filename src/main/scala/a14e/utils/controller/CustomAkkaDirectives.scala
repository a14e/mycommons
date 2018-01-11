package a14e.utils.controller

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
import akka.stream.Materializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.scalalogging.{LazyLogging, Logger}

import scala.collection.immutable
import scala.collection.mutable
import scala.language.implicitConversions


trait CustomAkkaDirectives {
  this: LazyLogging =>

  val pathBoolean: PathMatcher1[Boolean] = Segment.flatMap {
    case "true" => Some(true)
    case "false" => Some(false)
    case _ => None
  }

  def logData(enabled: Boolean): Directive0 = {
    if (!enabled) pass
    else {
      val requestId = "request-" + UUID.randomUUID().toString
        DebuggingDirectives.logRequestResult(requestId, Logging.InfoLevel) &
        respondWithDefaultHeader(RawHeader("Request-Id", requestId))
    }
  }

  def setCors(enabled: Boolean): Directive0 = {
    if (!enabled) pass
    else CorsDirectives
      .cors(CorsSettings.defaultSettings.copy(allowedMethods = allowedMethods))
  }

  def userAgentHeader: Directive1[Option[`User-Agent`]] =
    HeaderDirectives.optionalHeaderValueByType[`User-Agent`]((): Unit)

  private val allowedMethods = immutable.Seq(DELETE, GET, OPTIONS, PATCH, POST, PUT)
}




package a14e.utils.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._

object CustomDirectives {
  def wwwRedirect: Directive0 = {

    extractHost.flatMap { host =>
      if (!host.startsWith("www.")) pass
      else {
        extractUri.flatMap { uri =>
          val newUri = uri.toString().replaceFirst("www.", "")
          redirect(newUri, StatusCodes.PermanentRedirect): Directive0
        }

      }
    }
  }

  def forbidIframe: Directive0 = {
    respondWithHeader(RawHeader("X-Frame-Options", "SAMEORIGIN"))
  }



}

package a14e.commons.controller

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging


abstract class Controller extends CustomAkkaDirectives with LazyLogging {

  def route: Route
}

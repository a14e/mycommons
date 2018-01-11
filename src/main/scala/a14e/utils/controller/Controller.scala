package a14e.utils.controller

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging


abstract class Controller extends CustomAkkaDirectives with LazyLogging {

  def route: Route
}

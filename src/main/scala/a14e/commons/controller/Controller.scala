package a14e.commons.controller

import akka.http.scaladsl.server.{Directive0, Route}
import com.typesafe.scalalogging.LazyLogging


abstract class Controller extends CustomAkkaDirectives with LazyLogging {

  def route: Route

  def map(f: Route => Route): Controller = Controller.from {
    f(this.route)
  }

  def addDirective[T](directive: Directive0): Controller = this.map { route =>
    directive {
      route
    }
  }

  def flatMap(f: Route => Controller): Controller = {
    Controller.from {
      val route = this.route
      val controller = f(route)
      controller.route
    }
  }
}

object Controller {
  def from(x: => Route): Controller = new Controller {
    override def route: Route = x
  }
}



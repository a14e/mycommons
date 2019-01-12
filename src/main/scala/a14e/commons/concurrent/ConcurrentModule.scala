package a14e.commons.concurrent

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import a14e.commons.configs.ConfigurationModule
import com.softwaremill.macwire._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

trait ConcurrentModule {
  this: ConfigurationModule
  with LazyLogging =>

  implicit lazy val system: ActorSystem = ActorSystem("default-system", configuration)

  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  sys.addShutdownHook {
    logger.info(s"Shutting down actor system ${system.name}")
    system.terminate()
  }
}


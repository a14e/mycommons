package a14e.utils.concurrent

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import a14e.utils.configs.ConfigurationModule
import com.softwaremill.macwire._

import scala.concurrent.ExecutionContext

trait ConcurrentModule {
  this: ConfigurationModule =>

  lazy val synchronizationManagerFactoryImpl: SynchronizationManagerFactory = wire[SynchronizationManagerFactoryImpl]

  implicit lazy val system: ActorSystem = ActorSystem("default-system", configuration)

  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
}


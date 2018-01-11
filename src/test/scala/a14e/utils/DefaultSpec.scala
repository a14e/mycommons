package a14e.utils

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

trait DefaultSpec
  extends FlatSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar {
  self =>

  trait ConcurrentWirings {
    implicit def actorSystem: ActorSystem = self.actorSystem
    implicit def excecutionContext: ExecutionContext = self.excecutionContext
    implicit def materializer: Materializer = self.materializer
  }

  protected implicit val overridenPatienceConfig = PatienceConfig(Span(1, Seconds), Span(15, Millis))

  private implicit lazy val actorSystem = ActorSystem()
  private implicit lazy val excecutionContext = actorSystem.dispatcher
  private implicit lazy val materializer = ActorMaterializer()
}

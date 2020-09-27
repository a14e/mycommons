package a14e.commons

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

  protected implicit val overridenPatienceConfig = PatienceConfig(Span(1, Seconds), Span(15, Millis))

}

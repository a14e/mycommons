package a14e.commons.concurrent

import akka.actor.ActorSystem
import a14e.commons.DefaultSpec
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eq_, _}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

class ActorSynchronizationManagerImplSpec extends DefaultSpec with BeforeAndAfterAll {

  "sync" should "run only one task at same time" in new Wiring {
    val function = mock[(Int) => Int]
    when(function.apply(any())).thenReturn(1)
    val promise1 = Promise[Int]()
    val promise2 = Promise[Int]()
    val promise3 = Promise[Int]()
    manager.sync("key1")(promise1.future).foreach(_ => function(1))
    manager.sync("key1")(promise2.future).foreach(_ => function(2))
    manager.sync("key1")(promise3.future).foreach(_ => function(3))

    verify(function, never()).apply(any())
    promise1.success(1)
    verify(function, Mockito.timeout(100).times(1)).apply(1)
    verify(function, Mockito.timeout(20).times(1)).apply(any())
    promise2.success(2)
    Thread.sleep(10)
    verify(function, Mockito.timeout(50).times(1)).apply(2)
    verify(function, Mockito.timeout(20).times(2)).apply(any())
    promise3.success(3)
    Thread.sleep(10)
    verify(function, Mockito.timeout(50).times(1)).apply(3)
    verify(function, Mockito.timeout(20).times(3)).apply(any())
  }

  it should "call next tasks after complete" in new Wiring {
    val function = mock[(Int) => Int]
    when(function.apply(any())).thenReturn(1)
    val promise1 = Promise[Int]()
    val promise2 = Promise[Int]()
    manager.sync("key1")(promise1.future).foreach(_ => function(1))
    manager.sync("key1")(promise2.future).foreach(_ => function(2))

    promise2.success(2)
    Thread.sleep(100)
    verify(function, never()).apply(any())
    promise1.success(1)
    verify(function, Mockito.timeout(20).times(1)).apply(1)
    verify(function, Mockito.timeout(20).times(1)).apply(2)
    verify(function, Mockito.timeout(20).times(2)).apply(any())
  }

  it should "pass tasks with different keys" in new Wiring {
    val function = mock[(Int) => Int]
    when(function.apply(any())).thenReturn(1)
    val promise1 = Promise[Int]()
    val promise2 = Promise[Int]()
    manager.sync("key1")(promise1.future).foreach(_ => function(1))
    manager.sync("key2")(promise2.future).foreach(_ => function(2))

    promise2.success(2)
    verify(function, Mockito.timeout(50).times(1)).apply(2)
    verify(function, Mockito.timeout(50).times(1)).apply(any())
    promise1.success(1)
    verify(function, Mockito.timeout(50).times(1)).apply(1)
    verify(function, Mockito.timeout(50).times(2)).apply(any())
  }

  private lazy val actorSystem = ActorSystem()

  override def afterAll(): Unit = {
    actorSystem.terminate().futureValue
    super.afterAll()
  }

  trait Wiring {
    val manager = new ActorSynchronizationManagerImpl(actorSystem)
  }
}

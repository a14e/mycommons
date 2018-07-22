package a14e.commons.concurrent

import a14e.commons.DefaultSpec
import akka.actor.ActorSystem
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class AsyncMutexImplSpec extends DefaultSpec {

  "apply" should "run only one task at same time" in new Wiring {
    val function = mock[(Int) => Int]
    when(function.apply(any())).thenReturn(1)
    val promise1 = Promise[Int]()
    val promise2 = Promise[Int]()
    val promise3 = Promise[Int]()
    mutex(Await.result(promise1.future, Duration.Inf)).foreach(_ => function(1))
    mutex(Await.result(promise2.future, Duration.Inf)).foreach(_ => function(2))
    mutex(Await.result(promise3.future, Duration.Inf)).foreach(_ => function(3))

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
    mutex(Await.result(promise1.future, Duration.Inf)).foreach(_ => function(1))
    mutex(Await.result(promise2.future, Duration.Inf)).foreach(_ => function(2))

    promise2.success(2)
    Thread.sleep(100)
    verify(function, never()).apply(any())
    promise1.success(1)
    verify(function, Mockito.timeout(20).times(1)).apply(1)
    verify(function, Mockito.timeout(20).times(1)).apply(2)
    verify(function, Mockito.timeout(20).times(2)).apply(any())
  }

  "async" should "run only one task at same time" in new Wiring {
    val function = mock[(Int) => Int]
    when(function.apply(any())).thenReturn(1)
    val promise1 = Promise[Int]()
    val promise2 = Promise[Int]()
    val promise3 = Promise[Int]()
    mutex.async(promise1.future).foreach(_ => function(1))
    mutex.async(promise2.future).foreach(_ => function(2))
    mutex.async(promise3.future).foreach(_ => function(3))

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
    mutex.async(promise1.future).foreach(_ => function(1))
    mutex.async(promise2.future).foreach(_ => function(2))

    promise2.success(2)
    Thread.sleep(100)
    verify(function, never()).apply(any())
    promise1.success(1)
    verify(function, Mockito.timeout(20).times(1)).apply(1)
    verify(function, Mockito.timeout(20).times(1)).apply(2)
    verify(function, Mockito.timeout(20).times(2)).apply(any())
  }


  private lazy val actorSystem = ActorSystem()

  sys.addShutdownHook(actorSystem.terminate())

  trait Wiring {
    val mutex = new AsyncMutexImpl()(actorSystem.dispatcher, actorSystem)
  }
}

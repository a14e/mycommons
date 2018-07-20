package a14e.utils.concurrent

import akka.actor.ActorSystem

import scala.collection.mutable
import scala.concurrent.ExecutionContext

trait SynchronizationManagerFactory {

  def manager(name: String): SynchronizationManager

}


class SynchronizationManagerFactoryImpl(actorSystem: ActorSystem)
                                       (implicit context: ExecutionContext) extends SynchronizationManagerFactory {
  def manager(name: String): SynchronizationManager = this.synchronized {

    store.getOrElseUpdate(name, new ActorSynchronizationManagerImpl(actorSystem))

  }


  private lazy val store = new mutable.HashMap[String, SynchronizationManager]()

}
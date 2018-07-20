package a14e.utils.cache.manager

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import a14e.utils.cache.configuration.CacheManagerConfigs
import a14e.utils.cache.{AsyncCache, AsyncCacheImpl}
import a14e.utils.concurrent.SynchronizationManagerFactory

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait CacheManager {
  def cache[KEY <: AnyRef, VALUE <: AnyRef](name: String): AsyncCache[KEY, VALUE]

}


class CacheManagerImpl(allConfigs: CacheManagerConfigs,
                       synchronizationManagerFactory: SynchronizationManagerFactory)
                      (implicit
                       context: ExecutionContext,
                       materializer: Materializer) extends CacheManager {




  override def cache[KEY <: AnyRef, VALUE <: AnyRef](name: String): AsyncCache[KEY, VALUE] = this.synchronized {

    // todo поменять на гуаву
    def buildCache(): AsyncCache[AnyRef, AnyRef] = {
      val configs = allConfigs.namedCacheConfigs.getOrElse(name, allConfigs.defaultCacheConfigs)
      val syncManager = synchronizationManagerFactory.manager(name)
      val cache = new AsyncCacheImpl[KEY, VALUE](
        name,
        configs.maxSize,
        configs.ttl,
        syncManager
      )
      cache.asInstanceOf[AsyncCache[AnyRef, AnyRef]]
    }

    underlying.getOrElseUpdate(name, buildCache()).asInstanceOf[AsyncCache[KEY, VALUE]]
  }


  private val underlying = new mutable.HashMap[String, AsyncCache[AnyRef, AnyRef]]

}
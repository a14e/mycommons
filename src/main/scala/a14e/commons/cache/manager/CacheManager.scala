package a14e.commons.cache.manager

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import a14e.commons.cache.configuration.CacheManagerConfigs
import a14e.commons.cache.{AsyncCache, AsyncCacheImpl}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait CacheManager {
  def cache[KEY <: AnyRef, VALUE <: AnyRef](name: String): AsyncCache[KEY, VALUE]

}


class CacheManagerImpl(allConfigs: CacheManagerConfigs)
                      (implicit
                       context: ExecutionContext,
                       materializer: Materializer) extends CacheManager {




  override def cache[KEY <: AnyRef, VALUE <: AnyRef](name: String): AsyncCache[KEY, VALUE] = this.synchronized {

    def buildCache(): AsyncCache[AnyRef, AnyRef] = {
      val configs = allConfigs.namedCacheConfigs.getOrElse(name, allConfigs.defaultCacheConfigs)
      val cache = new AsyncCacheImpl[KEY, VALUE](
        name = name,
        maxSize = configs.maxSize,
        ttl = configs.ttl
      )
      cache.asInstanceOf[AsyncCache[AnyRef, AnyRef]]
    }

    underlying.getOrElseUpdate(name, buildCache()).asInstanceOf[AsyncCache[KEY, VALUE]]
  }


  // нужна ли вообще тут синхронизация?)
  private val underlying = new mutable.HashMap[String, AsyncCache[AnyRef, AnyRef]]

}
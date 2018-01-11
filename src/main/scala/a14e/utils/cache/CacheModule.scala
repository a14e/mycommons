package a14e.utils.cache

import a14e.utils.concurrent.ConcurrentModule
import a14e.utils.configs.ConfigurationModule
import com.softwaremill.macwire._
import a14e.utils.cache.configuration.CacheConfigsReader._
import a14e.utils.cache.configuration.CacheManagerConfigs
import a14e.utils.cache.manager.{CacheManager, CacheManagerImpl}
import net.ceedubs.ficus.Ficus._

trait CacheModule {
  this: ConcurrentModule
    with ConfigurationModule =>


  lazy val cacheManager: CacheManager = wire[CacheManagerImpl]
  lazy val cacheConfigs: CacheManagerConfigs = configuration.as[CacheManagerConfigs]("cache")

}

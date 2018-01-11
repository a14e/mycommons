package a14e.utils.cache.configuration

import scala.concurrent.duration.FiniteDuration
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

case class CacheConfigs(maxSize: Int,
                        ttl: FiniteDuration)

case class CacheManagerConfigs(namedCacheConfigs: Map[String, CacheConfigs],
                               defaultCacheConfigs: CacheConfigs)


object CacheConfigsReader {

  implicit lazy val cacheConfigs: ValueReader[CacheConfigs] = ValueReader.relative[CacheConfigs] { config =>
    CacheConfigs(
      maxSize = config.as[Int]("max-size"),
      ttl = config.as[FiniteDuration]("ttl")
    )
  }


  implicit lazy val cacheManagerConfigs: ValueReader[CacheManagerConfigs] = {
    ValueReader.relative[CacheManagerConfigs] { config =>

      CacheManagerConfigs(
        namedCacheConfigs = config.as[Map[String, CacheConfigs]]("caches"),
        defaultCacheConfigs = CacheConfigs(
          maxSize = config.as[Int]("max-size"),
          ttl = config.as[FiniteDuration]("ttl")
        )

      )

    }
  }
}


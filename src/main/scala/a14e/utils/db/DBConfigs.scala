package a14e.utils.db

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

class DBConfigs(configuration: Config) {

  lazy val mongoUrlString: String = configuration.as[String]("mongo.url")
  lazy val mongoDBName: String = configuration.as[String]("mongo.db")
}

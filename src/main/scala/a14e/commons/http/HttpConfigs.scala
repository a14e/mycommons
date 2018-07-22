package a14e.commons.http

import a14e.commons.configs.ConfigsKey
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

class HttpConfigs(config: Config, applicationName: String) {

  lazy val port: Int = config.as[Int](keys.Port)
  lazy val host: String = config.as[String](keys.Host)

  // internal
  private lazy val keys = new ConfigsKey(applicationName) {
    val Port: String = localKey("port")
    val Host: String = localKey("host")
  }
}

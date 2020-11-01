package a14e.commons.flyaway

import com.typesafe.config.Config
import pureconfig.ConfigSource

case class MigrationsConfigs(url: String,
                             login: String,
                             driver: String,
                             password: String,
                             directories: Seq[String],
                             dbName: Option[String],
                             migrationOnStart: Boolean)


object MigrationsConfigs {
  def from(config: Config, path: String = "migration"): MigrationsConfigs = {
    import pureconfig.generic.auto._

    ConfigSource.fromConfig(config)
      .at(path)
      .loadOrThrow[MigrationsConfigs]
  }

}




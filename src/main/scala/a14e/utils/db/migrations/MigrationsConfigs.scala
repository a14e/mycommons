package a14e.utils.db.migrations

import a14e.utils.configs.ConfigsKey
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

case class MigrationsConfigs(url: String,
                             login: String,
                             driver: String,
                             password: String,
                             directories: Seq[String],
                             dbName: Option[String],
                             migrationOnStart: Boolean)


object MigrationsConfigsReader {
  implicit val migrationsConfigsReader: ValueReader[MigrationsConfigs] =
    ValueReader.relative[MigrationsConfigs] { config =>
    import Keys._
    MigrationsConfigs(
      url = config.as[String](Url),
      login = config.as[String](Login),
      driver = config.as[String](Driver),
      password = config.as[String](Password),
      directories = config.as[Seq[String]](Directories).map(_.trim),
      dbName = config.as[Option[String]](DbName),
      migrationOnStart = config.getOrElse[Boolean](MigrationOnStart, false)
    )
  }


  object Keys {
    val Url: String = "url"
    val Login: String = "login"
    val Driver: String = "driver"
    val DbName: String = "db-name"
    val Password: String = "password"
    val Directories: String = "directories"
    val MigrationOnStart: String = "migrate-on-start"
  }
}




package a14e.utils.db

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.{ConfigValueReader, ValueReader}


case class MongoDBConfigs(url: String, db: String, user: Option[String], password: Option[String])

object MongoDBConfigsReaders {
  implicit lazy val MongoDBConfigsReader: ValueReader[MongoDBConfigs] = ValueReader.relative { config =>
    MongoDBConfigs(
      url = config.as[String]("url"),
      db = config.as[String]("db"),
      user = config.as[Option[String]]("user"),
      password = config.as[Option[String]]("password")
    )
  }
}
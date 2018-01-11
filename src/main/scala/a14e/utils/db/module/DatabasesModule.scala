package a14e.utils.db.module

import a14e.utils.configs.ConfigurationModule
import a14e.utils.db.DBConfigs
import com.softwaremill.macwire._
import org.mongodb.scala.{MongoClient, MongoDatabase}

trait DatabasesModule {
  this: ConfigurationModule =>

  lazy val mongoClient: MongoClient = MongoClient(dBConfigs.mongoUrlString)

  lazy val database: MongoDatabase =
    mongoClient.getDatabase(dBConfigs.mongoDBName)

  lazy val dBConfigs: DBConfigs = wire[DBConfigs]

  sys.addShutdownHook {
    mongoClient.close()
  }

}


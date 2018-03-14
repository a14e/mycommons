package a14e.utils.db.module

import java.net.InetSocketAddress
import java.util.UUID

import a14e.bson.ID
import a14e.utils.concurrent.ConcurrentModule
import a14e.utils.configs.ConfigurationModule
import a14e.utils.db.{DefaultDao, MongoDBConfigs}
import a14e.utils.db.MongoDBConfigsReaders._
//import a14e.utils.db.module.State.State
import a14e.utils.enum.FindableEnum
import akka.http.scaladsl.model.Uri
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.mongodb.scala.connection.ClusterSettings
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoCredential, MongoDatabase, ServerAddress}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait DatabasesModule {
  this: ConfigurationModule =>

  lazy val mongoClient: MongoClient = {
    (dBConfigs.user, dBConfigs.password) match {
      case (Some(user), Some(password)) =>


        val clusterSettings: ClusterSettings = {
          val uri = Uri(dBConfigs.url)
          val host = ServerAddress(uri.authority.host.toString(), uri.authority.port)

          ClusterSettings.builder().hosts(List(host).asJava).description("Local Server").build()
        }
        val credentials = MongoCredential.createCredential(
          user,
          dBConfigs.db,
          password.toCharArray
        )

        val settings = MongoClientSettings.builder()
          .clusterSettings(clusterSettings)
          .credential(credentials)
          .build()

        MongoClient(settings)
      case _ =>
        MongoClient(dBConfigs.url)

    }
  }

  lazy val database: MongoDatabase =
    mongoClient.getDatabase(dBConfigs.db)

  lazy val dBConfigs: MongoDBConfigs = configuration.as[MongoDBConfigs]("mongo")

  sys.addShutdownHook {
    mongoClient.close()
  }

}

//
//
//import a14e.bson.auto._
//import a14e.utils.bson.CustomBsonEncodings._
//
//case class User(id: ID[String],
//                name: String,
//                age: Int,
//                state: State)
//
//object State extends FindableEnum {
//  type State = Value
//  val Active = Value("ACTIVE")
//}
//
//object Test extends App with DatabasesModule
//  with ConcurrentModule
//  with ConfigurationModule {
//  override def configuration = ConfigFactory.parseString(
//    """
//      |mongo {
//      |  url = "mongodb://localhost:27017"
//      |  db = "dev"
//      | user = "userAdmin"
//      | pwd = "qwerty123"
//      |
//      |}
//      |
//      |
//    """.stripMargin
//  )
//
//  val dao = new DefaultDao[User, String](database, "users")
//
//  val user = User(UUID.randomUUID().toString, "Andrew", 25, State.Active)
//
//
//  val res = dao.insert(user)
//  Await.ready(res, Duration.Inf)
//
//  val found = dao.findById(user.id)
//  val sync = Await.ready(found, Duration.Inf)
//  println(sync)
//}
package a14e.commons.flyaway

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{IO, Resource, Sync}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.Flyway
import cats.implicits._
import org.flywaydb.core.internal.util.jdbc.DriverDataSource

import scala.concurrent.ExecutionContext

trait MigrationService[F[_]] {
  def migrate(): F[Unit]

  def migrateIfConfigured(): F[Boolean]

  def migrateByArguments(args: Array[String]): F[Boolean]
}


class MigrationServiceImpl[F[_] : Async](migrationConfigs: MigrationsConfigs, blockingContext: ExecutionContext)
  extends MigrationService[F]
    with LazyLogging {
  override def migrate(): F[Unit] = {
    val io = Async[F].defer {
      logger.info("Starting migrations")
      generateFlyaway().map(_.migrate())
    }

    Async[F].evalOn(io, blockingContext).void
  }

  override def migrateIfConfigured(): F[Boolean] = {
    val migrated = migrationConfigs.migrationOnStart
    if (migrationConfigs.migrationOnStart) migrate().as(migrated)
    else Sync[F].pure(migrated)
  }


  override def migrateByArguments(args: Array[String]): F[Boolean] = {
    val shouldMigrate = Option(System.getProperty(MigrationKeyword)).map(_.trim.toLowerCase).contains("true")
    if (shouldMigrate) migrate().as(shouldMigrate)
    else Sync[F].pure(shouldMigrate)
  }

  private def generateFlyaway(): F[Flyway] = Sync[F].delay {
    val flyaway = new Flyway()
    val classLoader = Thread.currentThread.getContextClassLoader
    val correctedUrl = {
      migrationConfigs.dbName.fold(migrationConfigs.url) { dbName =>
        removeTrailingSlash(migrationConfigs.url) + "/" + dbName
      }
    }
    val datasource = {
      new DriverDataSource(
        classLoader,
        migrationConfigs.driver,
        correctedUrl,
        migrationConfigs.login,
        migrationConfigs.password,
        null
      )
    }
    flyaway.setDataSource(datasource)
    flyaway.setBaselineOnMigrate(true)
    flyaway.setLocations(migrationConfigs.directories: _ *)
    flyaway
  }

  private def removeTrailingSlash(string: String): String = {
    if (string.endsWith("/")) string.dropRight(1)
    else string
  }

  private val MigrationKeyword = "migrate"

}

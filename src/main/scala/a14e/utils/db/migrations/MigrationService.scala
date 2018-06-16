package a14e.utils.db.migrations

import com.typesafe.config.{Config, ConfigFactory}
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.util.jdbc.DriverDataSource

trait MigrationService {
  def migrate(): Unit
  def migrateIfConfigured(): Unit
  def migrateByArguments(args: Array[String]): Unit
}


class MigrationServiceImpl(migrationConfigs: MigrationsConfigs) extends MigrationService {
  override def migrate(): Unit = {
    val flyaway = generateFlyaway()
    flyaway.migrate()
  }

  override def migrateIfConfigured(): Unit = {
    if(migrationConfigs.migrationOnStart)
      migrate()
  }


  override def migrateByArguments(args: Array[String]): Unit = {
    val shouldMigrate = args.map(_.trim).contains(MigrationKeyword)
    if(shouldMigrate)
      migrate()
  }

  private def generateFlyaway(): Flyway = {
    val flyaway = new Flyway()
    val classLoader = Thread.currentThread.getContextClassLoader
    val correctedUrl = {
      migrationConfigs.dbName.fold(migrationConfigs.url) { dbName =>
        removeTrailigSlash(migrationConfigs.url) + "/" + dbName
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
    flyaway.setLocations(migrationConfigs.directories: _ * )
    flyaway
  }

  private def removeTrailigSlash(string: String): String = {
    if(string.endsWith("/")) string.dropRight(1)
    else string
  }

  private val MigrationKeyword = "migrate"

}

package a14e.utils.db.migrations

import com.softwaremill.macwire._
import MigrationsConfigsReader._
import net.ceedubs.ficus.Ficus._
import a14e.utils.configs.ConfigurationModule

trait MigrationsModule {
  this: ConfigurationModule =>

  lazy val migrationsConfigsImpl: MigrationsConfigs = configuration.as[MigrationsConfigs]("migration")
  lazy val migrationsServiceImpl: MigrationService = wire[MigrationServiceImpl]
}

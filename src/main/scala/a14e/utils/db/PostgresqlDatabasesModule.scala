package a14e.utils.db

import a14e.utils.configs.ConfigurationModule
import com.typesafe.config.Config

class DbContext(configuration: Config)
  extends PostgresAsyncContext[NamingStrategy.type](NamingStrategy, configuration)
    with DbJavaTimeSupport
    with SymbolsSupport
    with DynamicQueryBuildingSupport
    with DbEnumsSupport
    with DbJsonSupport

case object NamingStrategy extends PluralizedTableNames with SnakeCase with LowerCase

trait PostgresqlDatabasesModule {
  this: ConfigurationModule =>

  lazy val ctx: DbContext = new DbContext(configuration.getConfig("db"))


  sys.addShutdownHook {
    ctx.close()
  }

}


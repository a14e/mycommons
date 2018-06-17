package a14e.utils.db

import a14e.utils.configs.ConfigurationModule
import com.typesafe.config.Config
import io.getquill.{LowerCase, NamingStrategy, PluralizedTableNames, PostgresAsyncContext, SnakeCase}

class DbContext[NS <: NamingStrategy](strategy: NS, configuration: Config)
  extends PostgresAsyncContext[NS](strategy, configuration)
    with DbJavaTimeSupport
    with SymbolsSupport
    with DynamicQueryBuildingSupport
    with DbEnumsSupport
    with DbJsonSupport

case object NamingStrategy extends PluralizedTableNames with SnakeCase with LowerCase

trait PostgresqlDatabasesModule[NS <: NamingStrategy] {
  this: ConfigurationModule =>

  def namingStrategy: NS

  lazy val ctx: DbContext[NS] = new DbContext[NS](namingStrategy, configuration.getConfig("db"))


  sys.addShutdownHook {
    ctx.close()
  }

}


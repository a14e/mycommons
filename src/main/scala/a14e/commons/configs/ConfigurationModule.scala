package a14e.commons.configs

import com.typesafe.config.Config


trait ConfigurationModule {

  def configuration: Config

}


trait ServerConfiguration {

  def enableLogging: Boolean
  def enableCors: Boolean
}
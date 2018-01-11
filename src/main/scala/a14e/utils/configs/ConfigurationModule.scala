package a14e.utils.configs

import com.typesafe.config.Config


trait ConfigurationModule {

  def configuration: Config

}


trait ServerConfiguration {

  def enableLogging: Boolean
  def enableCors: Boolean
}
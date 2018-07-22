package a14e.commons.crypto

import a14e.commons.configs.ConfigsKey
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

class CryptoConfigs(configuration: Config) {

  lazy val hashingAlgorithm: String = configuration.as[String](keys.HashingAlgorithm)
  lazy val secret: String = configuration.as[String](keys.Secret)
  lazy val bcryptRounds: Int = configuration.as[Int](keys.BcryptRounds)
  lazy val enabledSsl: Boolean = configuration.as[Boolean](keys.EnabledSsl)
  lazy val certificatePath: String = configuration.as[String](keys.CertificatePath)



  // internal
  private lazy val keys = new ConfigsKey("crypto") {
    val HashingAlgorithm: String = localKey("hashing-algorithm")
    val BcryptRounds: String = localKey("bcrypt-rounds")
    val Secret: String = localKey("secret")
    val CertificatePath: String = localKey("certificate-path")
    val EnabledSsl: String = localKey("enabled-ssl")
  }
}

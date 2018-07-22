package a14e.commons.http

import java.io.{FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import a14e.commons.crypto.CryptoConfigs
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

class SslConfigs(cryptoConfigs: CryptoConfigs)
                (implicit
                 context: ExecutionContext,
                 materializer: Materializer,
                 system: ActorSystem) {

  def connectionContext: ConnectionContext = {
    if (cryptoConfigs.enabledSsl) ConnectionContext.https(generateSslContext())
    else  Http().defaultServerHttpContext
  }


  def generateSslContext(): SSLContext = {
    val password: Array[Char] = cryptoConfigs.secret.toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = new FileInputStream(cryptoConfigs.certificatePath)

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

    sslContext
  }
}

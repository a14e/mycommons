package a14e.utils.crypto

import com.google.common.io.BaseEncoding
import a14e.utils.crypto.HmacAlgorithm.HmacAlgorithm
import a14e.utils.encodings.AsImplicits._
import a14e.utils.encodings.Base64.Base64
import a14e.utils.encodings.Hex.Hex
import akka.util.ByteString

object HmacHashing {

  import javax.crypto.Mac
  import javax.crypto.spec.SecretKeySpec

  def hashBytes(message: ByteString,
                secret: ByteString,
                algorithm: HmacAlgorithm): ByteString = {
    val secretKey = new SecretKeySpec(message.toArray, algorithm.toString)
    val mac = Mac.getInstance(algorithm.toString)
    mac.init(secretKey)
    val result: Array[Byte] = mac.doFinal(message.toArray)
    ByteString(result)
  }

}


object HmacAlgorithm extends Enumeration {
  type HmacAlgorithm = Value
  final val HmacMD5 = Value("HmacMD5")
  final val HmacSHA1 = Value("HmacSHA1")
  final val HmacSHA256 = Value("HmacSHA256")
  final val HmacSHA512 = Value("HmacSHA512")
}
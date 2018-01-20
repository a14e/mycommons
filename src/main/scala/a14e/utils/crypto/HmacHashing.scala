package a14e.utils.crypto

import com.google.common.io.BaseEncoding
import a14e.utils.crypto.HmacAlgorithm.HmacAlgorithm
import a14e.utils.encodings.AsImplicits._
import a14e.utils.encodings.Base64.Base64

object HmacHashing {

  import javax.crypto.Mac
  import javax.crypto.spec.SecretKeySpec

  def hash(message: String,
           secret: String,
           algorithm: HmacAlgorithm): String = {
    val secretKey = new SecretKeySpec(secret.getBytes, algorithm.toString)
    val mac = Mac.getInstance(algorithm.toString)
    mac.init(secretKey)
    val result: Array[Byte] = mac.doFinal(message.getBytes)
    result.as[Base64]
  }
}


object HmacAlgorithm extends Enumeration {
  type HmacAlgorithm = Value
  final val HmacSHA1 = Value("HmacSHA1")
  final val HmacSHA256 = Value("HmacSHA256")
  final val HmacSHA512 = Value("HmacSHA512")
}
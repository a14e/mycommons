package a14e.commons.crypto

import a14e.commons.crypto.HmacAlgorithms.HmacAlgorithm

object HmacHashing {

  import javax.crypto.Mac
  import javax.crypto.spec.SecretKeySpec

  def hashBytes(message: Array[Byte],
                secret: Array[Byte],
                algorithm: HmacAlgorithm): Array[Byte] = {
    val secretKey = new SecretKeySpec(secret, algorithm.toString)
    val mac = Mac.getInstance(algorithm.toString)
    mac.init(secretKey)
    val result: Array[Byte] = mac.doFinal(message)
    result
  }

}


object HmacAlgorithms extends Enumeration {
  type HmacAlgorithm = Value
  final val HmacMD5 = Value("HmacMD5")
  final val HmacSHA1 = Value("HmacSHA1")
  final val HmacSHA256 = Value("HmacSHA256")
  final val HmacSHA512 = Value("HmacSHA512")
}
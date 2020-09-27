package a14e.commons.services

import java.security.SecureRandom
import java.util.UUID

import com.google.common.io.BaseEncoding

import scala.collection.BuildFrom
import scala.collection.generic.CanBuildFrom
import scala.util.Random


trait RandomGeneratingService {
  def stringId(): String

  def generateUuid(): UUID

  def generateUuidString(): String

  def generateHex(lenght: Int): String

  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): C
}

class RandomGeneratingServiceImpl(random: Random) extends RandomGeneratingService {
  override def stringId(): String = generateUuidString()

  override def generateUuid(): UUID = UUID.randomUUID()

  override def generateHex(lenght: Int): String = {
    val bytes = Random.nextBytes(8)
    BaseEncoding.base16().lowerCase().encode(bytes)
  }

  override def generateUuidString(): String = generateUuid().toString

  override def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): C = {
    random.shuffle(xs)
  }

}


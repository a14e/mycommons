package a14e.commons.services

import java.security.SecureRandom
import java.util.UUID

import com.google.common.io.BaseEncoding

import scala.collection.BuildFrom
import scala.collection.generic.CanBuildFrom
import scala.util.Random


trait RandomService {
  def generateUuid(): UUID

  def generateHex(lenght: Int): String

  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): C
}

class RandomServiceImpl(random: Random) extends RandomService {
  override def generateUuid(): UUID = UUID.randomUUID()

  override def generateHex(length: Int): String = {
    val bytes = random.nextBytes(8)
    BaseEncoding.base16().lowerCase().encode(bytes)
  }

  override def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): C = {
    random.shuffle(xs)
  }

}

object RandomService {
  val default: RandomService = new RandomServiceImpl(Random)
}
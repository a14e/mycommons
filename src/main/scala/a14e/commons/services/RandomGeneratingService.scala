package a14e.commons.services

import java.security.SecureRandom
import java.util.UUID

import scala.collection.BuildFrom
import scala.collection.generic.CanBuildFrom
import scala.util.Random


trait RandomGeneratingService {
  def stringId(): String

  def generateUuid(): UUID

  def generateUuidString(): String

  def generatePassword(length: Int): String

  def prettyId(): String

  def generateNumber(): Int

  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): C
}

final class RandomGeneratingServiceImpl extends RandomGeneratingService {
  override def stringId(): String = generateUuidString()

  override def generatePassword(length: Int): String = {
    random.alphanumeric.map(_.toLower).take(length).mkString
  }

  override def generateUuid(): UUID = UUID.randomUUID()

  override def generateUuidString(): String = generateUuid().toString

  override def prettyId(): String = IdGenerator.prettyKey(random)

  override def generateNumber(): Int = random.nextInt()

  override def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): C = {
    random.shuffle(xs)
  }

  private lazy val random = new Random(new SecureRandom())
}

object IdGenerator {
  def prettyKey(random: Random): String = {
    val alphaPart1 = upperAlphaStream(random).take(4).mkString
    val alphaPart2 = upperAlphaStream(random).take(4).mkString
    val numPart = numStream(random).take(4).mkString

    s"$alphaPart1-$alphaPart2-$numPart"
  }

  private def numStream(random: Random): Stream[Char] = {
    val seq = '0' to '9'
    val len = seq.length
    Stream.continually(random.nextInt(len)).map(seq)
  }

  private def upperAlphaStream(random: Random): Stream[Char] = {
    val seq = 'A' to 'Z'
    val len = seq.length
    Stream.continually(random.nextInt(len)).map(seq)
  }
}

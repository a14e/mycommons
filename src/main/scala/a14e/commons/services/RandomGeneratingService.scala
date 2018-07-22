package a14e.commons.services

import java.security.SecureRandom
import java.util.UUID



import scala.util.Random


trait RandomGeneratingService {
  def stringId(): String

  def generateUuid(): UUID


  def generatePassword(length: Int): String

  def prettyId(): String

  def generateNumber(): Int
}

class RandomGeneratingServiceImpl extends RandomGeneratingService {
  override def stringId(): String = UUID.randomUUID().toString

  override def generatePassword(length: Int): String = {
    random.alphanumeric.map(_.toLower).take(length).mkString
  }

  override def generateUuid(): UUID = UUID.randomUUID()

  override def prettyId(): String = IdGenerator.prettyKey()

  override def generateNumber(): Int = random.nextInt()

  private lazy val random = new Random(new SecureRandom())
}

object IdGenerator {
  def prettyKey(): String = {
    val alphaPart1 = upperAlphaStream().take(4).mkString
    val alphaPart2 = upperAlphaStream().take(4).mkString
    val numPart = numStream().take(4).mkString

    s"$alphaPart1-$alphaPart2-$numPart"
  }

  private def numStream(): Stream[Char] = {
    val seq = '0' to '9'
    val len = seq.length
    Stream.continually(random.nextInt(len)).map(seq)
  }

  private def upperAlphaStream(): Stream[Char] = {
    val seq = 'A' to 'Z'
    val len = seq.length
    Stream.continually(random.nextInt(len)).map(seq)
  }

  private lazy val random = new Random(new SecureRandom())
}

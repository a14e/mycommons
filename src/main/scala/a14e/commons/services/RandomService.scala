package a14e.commons.services

import java.security.SecureRandom
import java.util.UUID

import cats.effect.Sync
import com.google.common.io.BaseEncoding

import scala.collection.BuildFrom
import scala.collection.generic.CanBuildFrom
import scala.util.Random


trait RandomService[F[_]] {
  def generateUuid(): F[UUID]

  def generateHex(lenght: Int): F[String]

  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): F[C]
}

class RandomServiceImpl[F[_] : Sync](random: Random) extends RandomService[F] {
  protected val F: Sync[F] = Sync[F]

  override def generateUuid(): F[UUID] = F.delay(UUID.randomUUID())

  override def generateHex(length: Int): F[String] = F.delay {
    val bytes = random.nextBytes(8)
    BaseEncoding.base16().lowerCase().encode(bytes)
  }

  override def shuffle[T, C](xs: IterableOnce[T])(implicit bf: BuildFrom[xs.type, T, C]): F[C] = F.delay {
    random.shuffle(xs)
  }

}

object RandomService {
  def default[F[_] : Sync](): RandomService[F] = new RandomServiceImpl[F](new Random())

  def secure[F[_] : Sync](): RandomService[F] = new RandomServiceImpl[F](new Random(new SecureRandom()))
}

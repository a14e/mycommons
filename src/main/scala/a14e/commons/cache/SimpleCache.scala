package a14e.commons.cache

import java.util.concurrent.TimeUnit

import cats.effect.{Async, Effect, Sync}
import com.github.benmanes.caffeine.cache.Caffeine
import cats.syntax.all._

import scala.concurrent.Future
import scala.concurrent.duration._

trait SimpleCache[F[_], KEY, VALUE] {

  def cached(key: KEY)(value: => F[VALUE]): F[VALUE]

  def get(key: KEY): F[Option[VALUE]]

  def contains(key: KEY): F[Boolean]

  def put(key: KEY,
          value: VALUE): F[Unit]

  def remove(key: KEY): F[Unit]
}

class GuavaCache[F[_]: Async, KEY, VALUE](maxSize: Int = 10000,
                                                  ttl: FiniteDuration = 10.minutes) extends SimpleCache[F, KEY, VALUE] {

  private val F = Async[F]

  override def get(key: KEY): F[Option[VALUE]] = {
    F.delay {
      val res = underlying.getIfPresent()
      Option(res)
    }
  }

  override def contains(key: KEY): F[Boolean] = F.delay(underlying.getIfPresent(key) != null)

  override def put(key: KEY, value: VALUE): F[Unit] = F.delay(underlying.put(key, value))

  override def remove(key: KEY): F[Unit] = F.delay(underlying.invalidate(key))


  def cached(key: KEY)(block: => F[VALUE]): F[VALUE] = F.suspend {
    underlying.getIfPresent(key) match {
      case null =>
        // no sync
        block.map { x =>
          underlying.put(key, x)
          x
        }
      case x => F.pure(x)
    }

  }

  private val underlying = Caffeine.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[KEY, VALUE]()

}

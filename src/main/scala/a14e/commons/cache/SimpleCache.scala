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

class GuavaCache[F[_]: Async: Effect, KEY, VALUE](maxSize: Int = 10000,
                                                  ttl: FiniteDuration = 10.minutes) extends SimpleCache[F, KEY, VALUE] {

  private val F = Async[F]

  override def get(key: KEY): F[Option[VALUE]] = {
    F.suspend {
      underlying.getIfPresent() match {
        case null => F.pure(None)
        case res => res.map(Some(_))
      }
    }
  }

  override def contains(key: KEY): F[Boolean] = F.delay(underlying.getIfPresent(key) != null)

  override def put(key: KEY, value: VALUE): F[Unit] = F.delay(underlying.put(key, F.pure(value)))

  override def remove(key: KEY): F[Unit] = F.delay(underlying.invalidate(key))


  def cached(key: KEY)(block: => F[VALUE]): F[VALUE] = F.suspend {
    underlying.get(key, {
      _: KEY => Effect.toIOFromRunAsync(Async.memoize(F.suspend(block))).unsafeRunSync()
    }).handleErrorWith {
      err =>
        F.delay(underlying.invalidate(key)) *>
          F.raiseError(err)
    }
  }

  private val underlying = Caffeine.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[KEY, F[VALUE]]()

}

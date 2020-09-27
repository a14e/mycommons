package a14e.robobpm.utils.cache

import java.util.concurrent.TimeUnit

import cats.effect.{Async, IO, Sync}
import com.github.benmanes.caffeine.cache.Caffeine
import cats.syntax.all._
import scala.concurrent.Future
import scala.concurrent.duration._

trait SimpleCache[KEY, VALUE] {

  def cached(key: KEY)(value: => IO[VALUE]): IO[VALUE]

  def get(key: KEY): IO[Option[VALUE]]

  def contains(key: KEY): IO[Boolean]

  def put(key: KEY,
          value: VALUE): IO[Unit]

  def remove(key: KEY): IO[Unit]
}

class GuavaCache[KEY, VALUE](maxSize: Int = 10000,
                             ttl: FiniteDuration = 10.minutes) extends SimpleCache[KEY, VALUE] {


  override def get(key: KEY): IO[Option[VALUE]] = {
    IO.suspend {
      underlying.getIfPresent() match {
        case null => IO.pure(None)
        case res => res.map(Some(_))
      }
    }
  }

  override def contains(key: KEY): IO[Boolean] = IO(underlying.getIfPresent(key) != null)

  override def put(key: KEY, value: VALUE): IO[Unit] = IO(underlying.put(key, IO.pure(value)))

  override def remove(key: KEY): IO[Unit] = IO(underlying.invalidate(key))


  def cached(key: KEY)(block: => IO[VALUE]): IO[VALUE] = IO.suspend {
    underlying.get(key, {
      _: KEY => Async.memoize(IO.suspend(block)).unsafeRunSync()
    }).handleErrorWith {
      err =>
        IO.delay(underlying.invalidate(key)) *>
          IO.raiseError(err)
    }
  }

  private val underlying = Caffeine.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[KEY, IO[VALUE]]()

}

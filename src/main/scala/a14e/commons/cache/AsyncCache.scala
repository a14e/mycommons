package a14e.commons.cache

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.concurrent.ExecutionContext
import com.github.benmanes.caffeine.cache.Caffeine

import scala.util.control.NonFatal

trait AsyncCache[KEY <: AnyRef, VALUE <: AnyRef] {

  def get(key: KEY): Future[Option[VALUE]]

  def contains(key: KEY): Future[Boolean]

  def put(key: KEY,
          value: VALUE): Future[Unit]

  def remove(key: KEY): Future[Unit]

  def cached(key: KEY)(block: => Future[VALUE]): Future[VALUE]

}

// TODO возможно на других имплементация попробовать?
class AsyncCacheImpl[KEY <: AnyRef, VALUE <: AnyRef](name: String,
                                                     maxSize: Int,
                                                     ttl: FiniteDuration)
                                                    (implicit
                                                     context: ExecutionContext) extends AsyncCache[KEY, VALUE] {


  override def get(key: KEY): Future[Option[VALUE]] = {
    val found = underlying.getIfPresent(key)
    if (found == null) Future.successful(None)
    else found.map(Some.apply)(ExecutionContext.parasitic)
  }

  override def put(key: KEY,
                   value: VALUE): Future[Unit] =  {
    Future.successful(underlying.put(key, Future.successful(value)))
  }

  override def remove(key: KEY): Future[Unit] = {
    Future.successful(underlying.invalidate(key))
  }

  override def contains(key: KEY): Future[Boolean] = {
    val hasKey = underlying.getIfPresent(key) != null
    Future.successful(hasKey)
  }

  // TODO сделать тут каффеин для синхронизации (нужно ли?)
  override def cached(key: KEY)(block: => Future[VALUE]): Future[VALUE] = {
    underlying.get(key, {
      _: KEY =>
        try block catch {
          case NonFatal(e) => Future.failed(e)
        }
    }).andThen {
      case Failure(_) => underlying.invalidate(key)
    }
  }

  // TODO убрать транзакционну память
  private val underlying = Caffeine.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[KEY, Future[VALUE]]()

}

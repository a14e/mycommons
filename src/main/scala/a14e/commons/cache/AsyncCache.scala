package a14e.commons.cache

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import ch.qos.logback.core.util.TimeUtil
import a14e.commons.concurrent.SynchronizationManager
import com.google.common.cache.CacheBuilder

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.Success
import scala.async.Async._

trait AsyncCache[KEY <: AnyRef, VALUE <: AnyRef] {

  def get(key: KEY): Future[Option[VALUE]]

  def put(key: KEY,
          value: VALUE): Future[Unit]

  def remove(key: KEY): Future[Unit]

  def cached(key: KEY)(block: => Future[VALUE]): Future[VALUE]

}

// TODO возможно на других имплементация попробовать?
class AsyncCacheImpl[KEY <: AnyRef, VALUE <: AnyRef](name: String,
                                                     maxSize: Int,
                                                     ttl: FiniteDuration,
                                                     synchronizationManager: SynchronizationManager)
                                                    (implicit
                                                     context: ExecutionContext) extends AsyncCache[KEY, VALUE] {


  override def get(key: KEY): Future[Option[VALUE]] = {
    val found = Option(underlying.getIfPresent(key))
    Future.successful(found)
  }

  override def put(key: KEY,
                   value: VALUE): Future[Unit] =  {
    underlying.put(key, value)
    Future.unit
  }

  override def remove(key: KEY): Future[Unit] = {
    underlying.invalidate(key)
    Future.unit
  }


  // TODO сделать тут кофеин
  override def cached(key: KEY)(block: => Future[VALUE]): Future[VALUE] = {
    // двойная проверка, чтобы не нарываться лишний раз на синхронизацию
    get(key).flatMap {
      case Some(value) => Future.successful(value)
      case _ =>
        synchronizationManager.sync(key) { // тут возможны коллизии, но мы с ними готовы мириться)
          get(key).flatMap {
            case Some(res) => Future.successful(res)
            case _ => block.andThen {
              case Success(v) => put(key, v)
            }
          }
        }
    }


  }

  // TODO убрать транзакционну память
  private val underlying = CacheBuilder.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[KEY, VALUE]()
}

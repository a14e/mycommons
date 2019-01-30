package a14e.commons.cache

import java.util.concurrent.{Callable, TimeUnit}

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import ch.qos.logback.core.util.TimeUtil
import com.google.common.cache.CacheBuilder

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import scala.async.Async._
import a14e.commons.concurrent.FutureImplicits._
import a14e.commons.concurrent.FutureUtils

import scala.util.control.NonFatal

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
                                                     ttl: FiniteDuration)
                                                    (implicit
                                                     context: ExecutionContext) extends AsyncCache[KEY, VALUE] {


  override def get(key: KEY): Future[Option[VALUE]] = {
    val found = underlying.getIfPresent(key)
    if(found == null) Future.successful(None)
    else found.map(Some.apply)(FutureUtils.sameThreadExecutionContext)
  }

  override def put(key: KEY,
                   value: VALUE): Future[Unit] =  {
    Future.handle(underlying.put(key, Future.successful(value)))
  }

  override def remove(key: KEY): Future[Unit] = {
    Future.handle(underlying.invalidate(key))
  }


  // TODO сделать тут каффеин для синхронизации (нужно ли?)
  override def cached(key: KEY)(block: => Future[VALUE]): Future[VALUE] = {
    underlying.get(key, () => block).andThen {
      case Failure(_) => underlying.invalidate(key)
    }
  }

  // TODO убрать транзакционну память
  private val underlying = CacheBuilder.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[KEY, Future[VALUE]]()
}

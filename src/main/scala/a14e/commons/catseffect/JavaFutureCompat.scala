package a14e.commons.catseffect

import java.util.concurrent.{CancellationException, CompletableFuture, CompletionException}
import cats.effect.Async
import cats.implicits._


object JavaFutureCompat {

  // TODO убрать отсюда прокидывание контекста
  implicit class RichCompletableFuture[T](completableFuture: CompletableFuture[T]) {
    def to[F[_] : Async]: F[T] = {
      futureToIo[F, T](completableFuture)
    }
  }

  private def futureToIo[F[_] : Async, T](completableFuture: CompletableFuture[T]): F[T] = {
    // взял тут https://github.com/typelevel/cats-effect/issues/160
    Async[F].async_[T] { cb =>
      completableFuture.handle { (result, err) =>
        err match {
          case null => cb(Right(result))
          case _: CancellationException => ()
          case ex: CompletionException if ex.getCause ne null => cb(Left(ex.getCause))
          case ex => cb(Left(ex))
        }
      }
    }
  }
}

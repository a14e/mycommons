package a14e.robobpm.utils.catseffect

import java.util.concurrent.{CancellationException, CompletableFuture, CompletionException}

import cats.effect.{Async, ContextShift, Sync}
import cats.implicits._

import scala.language.higherKinds

object JavaFutureCompat {

  implicit class RichCompletableFuture[T](f: CompletableFuture[T]) {
    def to[F[_] : Async: ContextShift]: F[T] = {
      Sync[F].bracket(MdcEffect.getMdc[F]()) { _ =>
        futureToIo[F, T](f)
      }(mdc => ContextShift[F].shift *> MdcEffect.setMdc(mdc)) /* мы хотим убежать из контекста фьючи как можно раньше */
    }
  }

  private def futureToIo[F[_] : Async, T](completableFuture: CompletableFuture[T]): F[T] = {
    // взял тут https://github.com/typelevel/cats-effect/issues/160
    Async[F].async[T] { cb =>
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

package a14e.commons.validation

import cats.data.WriterT
import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Sync}
import cats.{Applicative, ApplicativeError, Functor, MonadError, Traverse}
import cats.instances.list._

import scala.language.higherKinds
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

object WriterValidationTest extends App {

  type Validator[F[_], RES] = WriterT[F, Vector[Throwable], RES]
  val Validator = WriterT

  implicit class RichValidatorObj(obj: Validator.type) {
    def ensure[F[_] : Applicative](cond: => Boolean)(err: => Throwable): Validator[F, Unit] = {
      try {
        if (cond) this.error(err)
        else this.empty
      } catch {
        case NonFatal(e) => error[F](e)
      }
    }

    def error[F[_] : Applicative](err: => Throwable): Validator[F, Unit] = {
      try {
        Validator.tell(Vector(err))
      } catch {
        case NonFatal(e) => error[F](e)
      }
    }

    def expression[F[_] ](exp: => F[_])(implicit monadError: MonadError[F, Throwable]): Validator[F, Unit] = {
      import cats.implicits._
      try {
        Validator.apply {
          exp.attempt.map {
            case Left(err) => (Vector(err), ())
            case Right(_) => (Vector.empty, ())
          }
        }
      } catch {
        case NonFatal(e) => error[F](e)
      }
    }

    def delay[F[_] : Applicative](exp: => Unit): Validator[F, Unit] = {
      try {
        Validator.pure(exp)
      } catch {
        case NonFatal(e) => error[F](e)
      }
    }

    def pure[F[_] : Applicative, RES](x: RES): Validator[F, RES] = {
      Validator.value(x)
    }

    def fromTry[F[_] : Applicative](t: => Try[_]): Validator[F, Unit] = {
      Try(t).flatten match {
        case Failure(exception) => error[F](exception)
        case Success(_) => empty[F]
      }
    }

    def empty[F[_] : Applicative]: Validator[F, Unit] = Validator.tell(Vector.empty)
  }

  implicit class RichValidator[F[_], RES](validator: Validator[F, RES]) {

    import cats.implicits._

    def firstError[B](implicit F: Functor[F]): F[Option[Throwable]] = {
      validator.written.map(_.headOption)
    }

    def onErrors[B](f: Vector[Throwable] => Unit)(implicit F: Functor[F]): F[Unit] = {
      validator.written.map { errs =>
        if (errs.nonEmpty)
          f(errs)
      }
    }
  }

  private implicit val runtime = IORuntime.global

  val res = (for {
    _ <- Traverse[List].traverse(List(1, 2, 3)) { i =>
      Validator.error[IO](new RuntimeException("1 + " + i))
    }
    _ <- Validator.error[IO](new RuntimeException("2"))
  } yield ()).written.unsafeRunSync()

  println(res)
}

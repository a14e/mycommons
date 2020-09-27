package a14e.commons.stream

import scala.language.higherKinds

object FS2Implicits {

  implicit class RichFs2Stream[F[_], T](stream: fs2.Stream[F, T]) {
    def withFilter(f: T => Boolean): fs2.Stream[F, T] = stream.filter(f)
  }

}

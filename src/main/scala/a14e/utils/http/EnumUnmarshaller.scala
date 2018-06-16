package a14e.utils.http

import a14e.utils.enum.EnumFinder
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.concurrent.Future
import scala.util.Try

object EnumUnmarshaller {
  implicit def enumerationValueUnmarshaller[T <: Enumeration : EnumFinder]: Unmarshaller[String, T#Value] = {
    Unmarshaller[String, T#Value] { _ =>
      str =>
        val res = Try(implicitly[EnumFinder[T]].find.withName(str))
        Future.fromTry(res)
    }
  }
}



package a14e.commons.http

import a14e.commons.enum.EnumFinder
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



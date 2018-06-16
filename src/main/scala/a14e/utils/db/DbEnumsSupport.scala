package a14e.utils.db

import a14e.utils.enum.EnumFinder
import io.getquill.MappedEncoding

trait DbEnumsSupport {
  implicit def enumEncoder[T <: Enumeration]: MappedEncoding[T#Value, String] =
    MappedEncoding[T#Value, String] { x: T#Value =>
      x.toString
    }

  implicit def enumDecoder[T <: Enumeration : EnumFinder]: MappedEncoding[String, T#Value] =
    MappedEncoding[String, T#Value] { name: String =>
      implicitly[EnumFinder[T]].find.withName(name)
    }
}


object DbEnumsSupport extends DbEnumsSupport
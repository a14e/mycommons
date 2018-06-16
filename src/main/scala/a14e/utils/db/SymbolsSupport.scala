package a14e.utils.db

trait SymbolsSupport {
  this: AsyncContext[_, _, _] =>

  implicit lazy val scalaSymbolEncoder: MappedEncoding[Symbol, String] = MappedEncoding[Symbol, String](_.name)

  implicit lazy val scalaSymbolDecoder: MappedEncoding[String, Symbol] = MappedEncoding[String, Symbol](Symbol(_))

}



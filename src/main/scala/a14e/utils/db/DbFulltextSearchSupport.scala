package a14e.utils.db

import io.getquill.context.async.AsyncContext

trait DbFulltextSearchSupport {
  this: AsyncContext[_, _, _] =>

  final val to_tsvector1 = quote {
    (data: String) => infix"to_tsvector($data)".as[TSvectorResult]
  }

  final val to_tsvector = quote {
    (language: String, data: String) => infix"to_tsvector($language, $data)".as[TSvectorResult]
  }

  final val to_tsquery1 = quote {
    (data: String) => infix"to_tsquery($data)".as[TSqueryResult]
  }

  final val to_tsquery = quote {
    (language: String, data: String) => infix"to_tsquery($language, $data)".as[TSqueryResult]
  }

  final val plainto_tsquery1 = quote {
    (data: String) => infix"plainto_tsquery($data)".as[TSqueryResult]
  }

  final val plainto_tsquery = quote {
    (language: String, data: String) => infix"plainto_tsquery($language, $data)".as[TSqueryResult]
  }



  implicit class FulltextQuotes(left: TSvectorResult) {
    def @@(right: TSqueryResult) = quote(infix"$left @@ $right".as[Boolean])
  }


  def prepareQueryToSearch(query: String): String = {
    val splitPattern = "[\\p{Punct}|\\s+]"
    query.split(splitPattern)
      .iterator
      .map(_.trim)
      .filterNot(_.isEmpty)
      .mkString(" & ")
  }

}

trait TSvectorResult
trait TSqueryResult


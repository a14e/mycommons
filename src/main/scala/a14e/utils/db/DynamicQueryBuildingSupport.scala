package a14e.utils.db

import io.getquill.context.async.AsyncContext

trait DynamicQueryBuildingSupport {
  this: AsyncContext[_, _, _] =>


  case class QueryBuilder[T](query: Quoted[Query[T]]) {

    def filterOption[U: Encoder](value: Option[U])(f: Quoted[(Query[T], U) => Query[T]]) =
      value match {
        case None => this
        case Some(v) =>
          QueryBuilder(
            quote {
              f(query, lift(v))
            }
          )
      }

    def doIf(ifCond: Boolean)(f: Quoted[Query[T] => Query[T]]) = {
      if (ifCond) QueryBuilder(quote {
        f(query)
      })
      else this
    }

    def take(take: Int) = {
      QueryBuilder(quote {
        query.take(lift(take))
      })
    }

    def takeOption(takeOpt: Option[Int]) =
      takeOpt match {
        case None => this
        case Some(take) =>
          QueryBuilder(quote {
            query.take(lift(take))
          })
      }

    def drop(drop: Int) =
      QueryBuilder(quote {
        query.take(lift(drop))
      })


    def dropOption(dropOpt: Option[Int]) =
      dropOpt match {
        case None => this
        case Some(drop) =>
          QueryBuilder(quote {
            query.take(lift(drop))
          })
      }

    def build = query
  }


}


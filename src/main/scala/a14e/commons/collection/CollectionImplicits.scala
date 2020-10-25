package a14e.commons.collection

import scala.collection.mutable

object CollectionImplicits {

  implicit class RichMap[K, V](val map: Map[K, V]) extends AnyVal {
    def mapKeys[NK](f: K => NK): Map[NK, V] = map.iterator.map { case (k, v) => f(k) -> v }.toMap


    def filterValues(f: V => Boolean): Map[K, V] = map.iterator.collect { case (k, v) if f(v) => k -> v }.toMap


    def revertKeysAndValues: Map[V, K] = map.map { case (k, v) => v -> k }

  }


  implicit class RichTraversable[ENTRY, COLL](val xs: COLL)
                                             (implicit conv: COLL <:< Iterable[ENTRY]) {



    def collectMaxBy[CHECK_KEY](pf: PartialFunction[ENTRY, CHECK_KEY])
                               (implicit cmp: Ordering[CHECK_KEY]): Option[ENTRY] = {
      var max = null.asInstanceOf[ENTRY]
      var maxKey = null.asInstanceOf[CHECK_KEY]
      for (candidate <- xs)
        pf.applyOrElse[ENTRY, Any](candidate, _ => FakeObject) match {
          case FakeObject =>
          case candidateKey: CHECK_KEY@unchecked =>
            if (max == null || cmp.compare(candidateKey, maxKey) > 0) {
              max = candidate
              maxKey = candidateKey
            }
        }
      Option(max)
    }

    def collectMinBy[T](pf: PartialFunction[ENTRY, T])(implicit cmp: Ordering[T]): Option[ENTRY] = {
      collectMaxBy(pf)(cmp.reverse)
    }


  }

  protected case object FakeObject

}

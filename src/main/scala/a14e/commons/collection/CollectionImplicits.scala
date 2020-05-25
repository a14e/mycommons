package a14e.commons.collection

object CollectionImplicits {

  implicit class RichMap[K, V](val map: Map[K, V]) extends AnyVal {
    def mapKeys[NK](f: K => NK): Map[NK, V] = map.map { case (k, v) => f(k) -> v }

    def filterValues(f: V => Boolean): Map[K, V] = map.collect { case (k, v) if f(v) => k -> v }
  }
}

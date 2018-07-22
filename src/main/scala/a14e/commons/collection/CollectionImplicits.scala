package a14e.commons.collection

import scala.collection.{mutable, TraversableLike}
import scala.collection.generic.CanBuildFrom

object CollectionImplicits {

  implicit class RichMap[K, V](val map: Map[K, V]) extends AnyVal {
    def mapKeys[NK](f: K => NK): Map[NK, V] = map.map { case (k, v) => f(k) -> v }

    def filterValues(f: V => Boolean): Map[K, V] = map.collect { case (k, v) if f(v) => k -> v }
  }


  implicit class RichTraversable[A, B](val xs: B)
                                      (implicit conv: B <:< TraversableOnce[A]) {
    def distinctBy[C](toKey: A => C)
                     (implicit
                      cbf: CanBuildFrom[B, A, B]): B = {
      val builder = cbf(xs)
      val elems = new mutable.HashSet[C]()
      for (x <- xs) {
        val k = toKey(x)
        if (!elems.contains(k)) {
          builder += x
          elems += k
        }
      }
      builder.result()
    }


    def maxByOpt[C: Ordering](f: A => C): Option[A] = if (xs.isEmpty) None else Some(xs.maxBy(f))

    def minByOpt[C: Ordering](f: A => C): Option[A] = if (xs.isEmpty) None else Some(xs.minBy(f))
  }


}

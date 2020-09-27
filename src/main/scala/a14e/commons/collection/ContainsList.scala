package a14e.commons.collection

/**
 * чтобы можно было делать так
 * {{{
 * val blackList = new ContainsList(1, 2, 3)
 *
 * value match {
 *  case blackList(true) => в черном списке
 *  case blackList(false) => не в черном списке
 * }
 * }}}
 * */
class ContainsList[T](xs: Set[T]) {
  def this(xs: T*) = this(xs.toSet)

  def unapply(arg: T): Option[Boolean] = Some(xs.contains(arg))
}
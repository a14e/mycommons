package a14e.commons.enum

import java.util.concurrent.atomic.AtomicReference

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait EnumFinder[ENUM <: Enumeration] {
  def find: ENUM
}


trait FindableEnum {
  self : Enumeration =>



  implicit val enumFinder: EnumFinder[self.type] = new EnumFinder[self.type] {

    override def find: self.type = self
  }
}

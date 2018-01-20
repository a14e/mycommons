package a14e.utils.enum

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.ConfigException.Generic

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait EnumFinder[T <: Enumeration#Value] {
  def find: Enumeration
}



trait FindableEnum extends Enumeration {
  self =>


  implicit val enumFinder: EnumFinder[Value] = new EnumFinder[Value] {
    override def find: Enumeration = self
  }
}

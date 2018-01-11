package a14e.utils.configs

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import com.typesafe.config.{Config, ConfigFactory}

object ConfigsUtils {
  def fromMap(values: Map[String, Any]): Config = {
    val prepared = convertValueToJava(values).asInstanceOf[java.util.Map[String, AnyRef]]
    ConfigFactory.parseMap(prepared)
  }


  private def convertValueToJava(anyRef: Any): AnyRef = anyRef match {
    case i: Int => Integer.valueOf(i)
    case s: Short => Integer.valueOf(s)
    case b: Byte => Integer.valueOf(b)
    case d: Double => java.lang.Double.valueOf(d)
    case f: Float => java.lang.Double.valueOf(f)
    case i: BigInt => i.bigInteger
    case d: BigDecimal => d.bigDecimal
    case o: Option[_] => o.map(convertValueToJava).orNull
    case i: FiniteDuration => i.toString()
    case s: String => s
    case b: Boolean => java.lang.Boolean.valueOf(b)
    case xs: collection.Map[_, _] => xs.map { case (k, v) => k.toString -> convertValueToJava(v) }.asJava
    case xs: TraversableOnce[_] => xs.map(convertValueToJava).toSeq.asJava
    case ref: AnyRef => ref
    case null => null
    case _ => throw new RuntimeException("unsupported configs type")
  }
}

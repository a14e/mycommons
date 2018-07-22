package a14e.commons.configs

abstract class ConfigsKey(prefix: String) {

  protected def localKey(key: String): String = Seq(prefix, key).mkString(".")

}

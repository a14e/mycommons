package a14e.utils.configs

abstract class ConfigsKey(prefix: String) {

  protected def localKey(key: String): String = Seq(prefix, key).mkString(".")

}

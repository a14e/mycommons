package a14e.commons.files

import a14e.commons.configs.ConfigsKey
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

class FilesConfig(configs: Config) {

  import Keys._

  val indexHtmlFile: String = configs.as[String](IndexHtml)
  val webFilesFolder: String = configs.as[String](WebFilesFolder)
  val diskFilesFolder: String = configs.as[String](DiskFilesFolder)
  val rootDiskFilesFolder: String = configs.as[String](RootDiskFilesFolder)

}

object Keys extends ConfigsKey("files") {
  val IndexHtml: String = localKey("index-html")
  val WebFilesFolder: String = localKey("files-folder.web-path")
  val DiskFilesFolder: String = localKey("files-folder.disk-path")
  val RootDiskFilesFolder: String = localKey("root-folder.disk-path")
}
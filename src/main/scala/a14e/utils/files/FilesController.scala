package a14e.utils.files

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.CacheDirectives.{`must-revalidate`, `no-cache`}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.Materializer
import a14e.utils.controller.Controller

class FilesController(filesConfig: FilesConfig)
                     (implicit
                      val system: ActorSystem,
                      val materializer: Materializer) extends Controller {

  override lazy val route: Route =
    respondWithHeader(`Cache-Control`(`no-cache`, `must-revalidate`)) {
      pathEndOrSingleSlash {
        indexRoute
      } ~
      pathPrefix(filesConfig.webFilesFolder) {
        getFromDirectory(filesConfig.diskFilesFolder)
      } ~
      getFromDirectory(filesConfig.rootDiskFilesFolder) ~
      indexRoute
    }


  // internal

  private val indexRoute = {
    getFromFile(filesConfig.indexHtmlFile)
  }
}

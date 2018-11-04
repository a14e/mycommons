package a14e.commons.swagger

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.CacheDirectives.{`must-revalidate`, `no-cache`}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.stream.Materializer
import a14e.commons.controller.Controller
import a14e.commons.http.HttpConfigs
import akka.http.scaladsl.server.{Directive, Directive1}
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.webjars.WebJarAssetLocator
import net.ceedubs.ficus.Ficus._

import scala.util.{Failure, Success, Try}


class SwaggerController(val configs: Config,
                        val mainConfigs: HttpConfigs,
                        val swaggerDocService: SwaggerDocService)
                       (implicit
                        val system: ActorSystem,
                        val materializer: Materializer) extends Controller {

  import akka.actor.ActorSystem
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.Route
  import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
  import akka.stream.Materializer
  import a14e.commons.controller.Controller

  def init(): Unit = {
    if (enabled)
      logs.info(s"see swagger on $swaggerUri")
  }

  override lazy val route: Route =
    if (!enabled)
      reject
    else {
      respondWithHeader(`Cache-Control`(`no-cache`, `must-revalidate`)) {
        swaggerFilesRoute ~
          pathPrefix("swagger") {
            swaggerDocService.routes
          }
      }
    }

  // internal

  private val swaggerFilesRoute =
    pathPrefix("swagger") {
      pathEndOrSingleSlash {
        redirectWithUri
      } ~ {
        getFromResourceDirectory("swagger")
      }
    }

  private def redirectWithUri: Route =
    (get & extractRequest & sslEnabledDirective) { (request, sslEnabled) =>
    val query = Query("url" -> swaggerJsonUriString(request.uri, sslEnabled))
    val newUri = Uri("swagger/index.html").withQuery(query)
    redirect(newUri, StatusCodes.TemporaryRedirect)
  }

  private def swaggerJsonUriString(uri: Uri,
                                   sslEnabled: Boolean) = {
    val scheme = if(sslEnabled) "https" else uri.scheme

    s"$scheme://${uri.authority}/swagger/api-docs/swagger.json"
  }

  private def sslEnabledDirective: Directive1[Boolean] = {
    optionalCookie(SslEnabledCookieName).map { cookie =>
      cookie.exists(x => Try(x.value.toBoolean).toOption.contains(true))
    }
  }

  val SslEnabledCookieName = "X-Ssl-Enabled"

  private lazy val swaggerUri = s"http://${mainConfigs.host}:${mainConfigs.port}/swagger"

  private lazy val logs = Logger[this.type]

  private lazy val enabled = configs.as[Option[Boolean]]("swagger.enabled").getOrElse(false)

}

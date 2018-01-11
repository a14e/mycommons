package a14e.utils.swagger

import scala.reflect.runtime.{universe => ru}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import a14e.utils.http.HttpConfigs
import com.typesafe.config.Config
import io.swagger.annotations.Api
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition
import org.reflections.Reflections

import scala.collection.JavaConverters._

class SwaggerDocService(system: ActorSystem,
                        configuration: Config,
                        mainConfigs: HttpConfigs) extends SwaggerHttpService  {
  override lazy val apiClasses: Set[Class[_]] = classesWithApiAnnotation().toSet
  override val host = s"${mainConfigs.host}:${mainConfigs.port}"
  override val apiDocsPath: String = "api-docs"
  override val info = Info(version = "1.0")
  override val externalDocs = Some(new ExternalDocs("Core Docs", "http://acme.com/docs"))
  override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())


  private def classesWithApiAnnotation(): Seq[Class[_]] = {
    val ref = new Reflections("a14e")
    val annotatedJavaList = ref.getTypesAnnotatedWith(classOf[Api])
    annotatedJavaList.asScala.toList
  }

}



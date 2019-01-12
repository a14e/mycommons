package a14e.commons.swagger

import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import io.swagger.annotations.Api
import io.swagger.models.{ExternalDocs, Scheme}
import io.swagger.models.auth.BasicAuthDefinition
import org.reflections.Reflections
import scala.collection.JavaConverters._

class SwaggerDocService(reflectionPath: String,
                        override val schemes: List[Scheme] =  List(Scheme.HTTP, Scheme.HTTPS)) extends SwaggerHttpService  {
  override lazy val apiClasses: Set[Class[_]] = classesWithApiAnnotation().toSet

  override val apiDocsPath: String = "api-docs"
  override val info = Info(version = "1.0")
  override val externalDocs = None
  override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())

  private def classesWithApiAnnotation(): Seq[Class[_]] = {
    val ref = new Reflections(reflectionPath)
    val annotatedJavaList = ref.getTypesAnnotatedWith(classOf[Api])
    annotatedJavaList.asScala.toList
  }

}



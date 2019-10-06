package a14e.commons.swagger

import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import javax.ws.rs.Path
import org.reflections.Reflections
import scala.jdk.CollectionConverters._

class SwaggerDocService(reflectionPath: String,
                        override val schemes: List[String] = List("http", "https"),
                        classes: Seq[Class[_]]) extends SwaggerHttpService {
  override lazy val apiClasses: Set[Class[_]] = classes.toSet

  override val apiDocsPath: String = "api-docs"
  override val info = Info(version = "1.0")
  override val externalDocs = None
  override val securitySchemes = Map("basicAuth" -> {
    val res = new SecurityScheme()
    res.setType(SecurityScheme.Type.HTTP)
    res.setScheme("basic")
    res
  })

}



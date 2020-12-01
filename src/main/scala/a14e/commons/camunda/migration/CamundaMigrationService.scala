package a14e.commons.camunda.migration

import java.nio.file.Paths

import a14e.commons.camunda.client.CamundaProtocol.ProcessFile
import a14e.commons.camunda.client.{CamundaClient}
import cats.effect.Sync
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner

trait CamundaMigrationService[F[_]] {

  def migrate(apiRoots: Seq[String]): F[Unit]

}


class CamundaMigrationServiceImpl[F[_] : Sync](client: CamundaClient[F]) extends CamundaMigrationService[F]
  with LazyLogging {

  import cats.implicits._

  import scala.jdk.CollectionConverters._


  override def migrate(apiRoots: Seq[String]): F[Unit] = {
    for {
      files <- Sync[F].delay(apiRoots.flatMap(listFiles))
      _ = if (files.isEmpty) throw new RuntimeException("No files have been found ")
      _ = {
        val fileNames = files.map(_.filename)
        logger.info(s"Found ${fileNames.length} files: ${fileNames.mkString(", ")}")
      }
      _ <- client.publishProcess("process_deployment", files)
    } yield ()
  }

  private def listFiles(root: String): Seq[ProcessFile]  = {
    new Reflections(root, new ResourcesScanner())
      .getResources(pattern)
      .asScala
      .toList
      .map { pathString =>
        val bytes = IOUtils.toByteArray(getClass.getClassLoader.getResourceAsStream(pathString))
        val fileName = Paths.get(pathString).getFileName.toString
        ProcessFile(fileName, bytes)
      }
  }



  private val pattern = ".*\\.(dmn|bpmn|cmmn)$".r.pattern


}

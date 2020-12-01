package a14e.commons.camunda.client

import CamundaProtocol._
import a14e.commons.camunda.configuration.CamundaConfigs
import cats.effect.Sync
import com.typesafe.config.Config
import io.circe.Json
import pureconfig.ConfigSource
import sttp.client3.SttpBackend
import sttp.model.StatusCode

trait CamundaClient[F[_]] {

  def publishProcess(deploymentName: String,
                     files: Seq[ProcessFile]): F[Unit]

  type ProcessId = String

  def startProcess(processDefinitionKey: String,
                   businessKey: String): F[ProcessId]

  def fetchAndLock(request: FetchAndLockRequest): F[Seq[ExternalTask[Json]]]

  def completeTask(taskId: String,
                   request: CompleteTaskRequest): F[Unit]


  def failureTask(taskId: String,
                  request: TaskFailureRequest): F[Unit]

  def extendLock(taskId: String,
                 request: ExtendLockRequest): F[Unit]

  def bpmnError(taskId: String,
                request: TaskBpmnErrorRequest): F[Unit]

}


class CamundaClientImpl[F[_] : Sync](configs: CamundaConfigs,
                                     backend: SttpBackend[F, Any]) extends CamundaClient[F] {

  import cats.implicits._
  import sttp.client3._
  import io.circe._
  import sttp.client3.circe._
  import CamundaFormats._

  override def publishProcess(name: String,
                              files: Seq[ProcessFile]): F[Unit] = {
    assert(files.nonEmpty, "Cant publish empty list of files")
    val paramParts = Seq(
      multipart("deployment-name", name),
      multipart("enable-duplicate-filtering", "true"),
      multipart("deploy-changed-only", "true"),
      multipart("deployment-source", "robobpm-client")
    )

    val fileParts = files.zipWithIndex.map {
      case (file, i) => multipart("filename" + i, file.bytes).fileName(file.filename)
    }
    val uri = uri"${configs.baseUrl}/deployment/create"

    basicRequest.post(uri)
      .multipartBody(paramParts ++ fileParts)
      .send(backend)
      .flatMap(failOnError)
      .void
  }

  override def startProcess(processDefinitionKey: String,
                            businessKey: String): F[ProcessId] = {
    case class IdOfProcess(id: String)
    implicit lazy val idDecoder: Decoder[IdOfProcess] = io.circe.generic.semiauto.deriveDecoder[IdOfProcess]
    val uri = uri"${configs.baseUrl}/process-definition/key/$processDefinitionKey/start"
    val json = Json.obj("businessKey" -> Json.fromString(businessKey))
    basicRequest.post(uri)
      .body(json)
      .response(asJson[IdOfProcess])
      .send(backend)
      .flatMap(failOnError)
      .map(_.body)
      .flatMap(Sync[F].fromEither)
      .map(_.id)
  }


  override def fetchAndLock(request: FetchAndLockRequest): F[Seq[ExternalTask[Json]]] = {

    val uri = uri"${configs.baseUrl}/external-task/fetchAndLock"
    basicRequest.post(uri)
      .body(request)
      .response(asJson[Seq[ExternalTask[Json]]])
      .send(backend)
      .flatMap(failOnError)
      .map(_.body)
      .flatMap(Sync[F].fromEither)
  }

  override def completeTask(taskId: String,
                            request: CompleteTaskRequest): F[Unit] = {
    val uri = uri"${configs.baseUrl}/external-task/$taskId/complete"
    basicRequest.post(uri)
      .body(request)
      .send(backend)
      .flatMap(failOnError)
      .void
  }

  override def extendLock(taskId: String,
                          request: ExtendLockRequest): F[Unit] = {
    val uri = uri"${configs.baseUrl}/external-task/$taskId/extendLock"
    basicRequest.post(uri)
      .body(request)
      .send(backend)
      .flatMap(failOnError)
      .void
  }

  override def failureTask(taskId: String,
                           request: TaskFailureRequest): F[Unit] = {
    val uri = uri"${configs.baseUrl}/external-task/$taskId/failure"
    basicRequest.post(uri)
      .body(request)
      .send(backend)
      .flatMap(failOnError)
      .void
  }

  override def bpmnError(taskId: String,
                         request: TaskBpmnErrorRequest): F[Unit] = {
    val uri = uri"${configs.baseUrl}/external-task/$taskId/bpmnError"
    basicRequest.post(uri)
      .body(request)
      .send(backend)
      .flatMap(failOnError)
      .void
  }

  private def failOnError[T](response: Response[T]): F[Response[T]] = {
    if (response.code != StatusCode.Ok && response.code != StatusCode.NoContent)
      Sync[F].raiseError(new RuntimeException(s"received error with code ${response.code} " + response.body.toString))
    else Sync[F].delay(response)
  }

}

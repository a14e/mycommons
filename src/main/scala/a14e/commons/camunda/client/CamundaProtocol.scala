package a14e.commons.camunda.client

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import a14e.commons.camunda.client.CamundaProtocol._
import io.circe.{Codec, Decoder, Json}

object CamundaProtocol {

  // https://docs.camunda.org/manual/latest/reference/rest/external-task/fetch/
  case class FetchAndLockRequest(workerId: String,
                                 maxTasks: Int,
                                 usePriority: Boolean,
                                 asyncResponseTimeout: Long, // millis
                                 topics: Seq[FetchAndLockRequestTopic])

  case class FetchAndLockRequestTopic(topicName: String,
                                      lockDuration: Long, // millis
                                      processDefinitionKeyIn: Seq[String])


  case class ExternalTask[JSON](activityId: String,
                                activityInstanceId: String,
                                errorMessage: Option[String],
                                errorDetails: Option[String],
                                executionId: String,
                                id: String,
                                lockExpirationTime: OffsetDateTime,
                                processDefinitionId: String,
                                processDefinitionKey: String,
                                processInstanceId: String,
                                tenantId: Option[String],
                                retries: Option[Int],
                                workerId: Option[String],
                                priority: Int,
                                topicName: String,
                                businessKey: String,
                                variables: JSON)

  // https://docs.camunda.org/manual/latest/reference/rest/external-task/post-complete/
  case class CompleteTaskRequest(workerId: String,
                                 variables: Json)

  //https://docs.camunda.org/manual/latest/reference/rest/external-task/post-extend-lock/
  case class ExtendLockRequest(workerId: String,
                               newDuration: Long) // millis from current moment

  // https://docs.camunda.org/manual/latest/reference/rest/external-task/post-failure/
  case class TaskFailureRequest(workerId: String,
                                errorMessage: String,
                                errorDetails: String,
                                retries: Int,
                                retryTimeout: Long)

  // https://docs.camunda.org/manual/latest/reference/rest/external-task/post-bpmn-error/
  case class TaskBpmnErrorRequest(workerId: String,
                                  errorCode: String,
                                  errorMessage: String,
                                  variables: Json)


  case class ProcessFile(filename: String,
                         bytes: Array[Byte])

}


object CamundaFormats {


  private val offsetFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  private implicit def offsetDateTimeParser: Decoder[OffsetDateTime] = Decoder[String].map { text =>
    OffsetDateTime.parse(text, offsetFormatter)
  }


  implicit lazy val FetchAndLockRequestCodec: Codec.AsObject[FetchAndLockRequest] = io.circe.generic.semiauto.deriveCodec[FetchAndLockRequest]
  implicit lazy val FetchAndLockRequestTopicCodec: Codec.AsObject[FetchAndLockRequestTopic] = io.circe.generic.semiauto.deriveCodec[FetchAndLockRequestTopic]
  implicit lazy val TaskResponseCodec: Codec.AsObject[ExternalTask[Json]] = io.circe.generic.semiauto.deriveCodec[ExternalTask[Json]]
  implicit lazy val CompleteTaskRequestCodec: Codec.AsObject[CompleteTaskRequest] = io.circe.generic.semiauto.deriveCodec[CompleteTaskRequest]
  implicit lazy val ExtendLockRequestCodec: Codec.AsObject[ExtendLockRequest] = io.circe.generic.semiauto.deriveCodec[ExtendLockRequest]
  implicit lazy val TakFailureRequestCodec: Codec.AsObject[TaskFailureRequest] = io.circe.generic.semiauto.deriveCodec[TaskFailureRequest]
  implicit lazy val TaskBpmnErrorRequestCodec: Codec.AsObject[TaskBpmnErrorRequest] = io.circe.generic.semiauto.deriveCodec[TaskBpmnErrorRequest]
}

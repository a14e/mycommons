package a14e.commons.camunda.dsl

import a14e.commons.context.LazyContextLogging
import a14e.commons.camunda.client.CamundaProtocol.ExternalTask
import a14e.commons.camunda.configuration.LoopSettings
import a14e.commons.camunda.dsl.TopicRoute.TopicBuilder
import a14e.commons.camunda.subscription.{BpmnError, FailureHandlingStrategy, Subscription, SubscriptionManager}
import cats.effect.{IO, Sync}
import fs2.Stream
import io.circe.{Decoder, Encoder, Json}

import scala.collection.mutable.ListBuffer


class TopicRoute[F[_]](val routes: List[Subscription[F]]) extends LazyContextLogging {

  import cats.implicits._
  import cats.effect.implicits._

  def ++(other: TopicRoute[F]): TopicRoute[F] = new TopicRoute[F](this.routes ++ other.routes)


  def run(manager: SubscriptionManager[F]): F[Unit] = {
    manager.run(routes)
  }

}

object TopicRoute {
  def build[F[_]](processDefKey: Seq[String])
                 (builder: TopicBuilder[F] => Unit): TopicRoute[F] = {
    val topicBuilder = new TopicBuilder[F](processDefKey)
    builder(topicBuilder)
    topicBuilder.build
  }

  class TopicBuilder[F[_]](val processDefKeys: Seq[String]) {
    def +=(s: Subscription[F]): TopicBuilder[F] = {
      buffer += s
      this
    }

    def build: TopicRoute[F] = new TopicRoute[F](buffer.result())

    private val buffer = new ListBuffer[Subscription[F]]
  }

  object TopicBuilder {
    def apply[F[_] : TopicBuilder]: TopicBuilder[F] = implicitly[TopicBuilder[F]]
  }

}

object Routes {

  import cats.implicits._

  def route[
    F[_] : TopicBuilder : Sync,
    IN: Decoder,
    OUT: Encoder](topic: String,
                  differentLoop: Option[LoopSettings] = None,
                  sendDiffOnly: Boolean = true,
                  extendLock: Boolean = false,
                  failureHandlingStrategy: FailureHandlingStrategy = FailureHandlingStrategy.expRetries())
                 (handle: IN => F[OUT]): Unit = {
    routeCtx[F, IN, OUT](
      topic,
      differentLoop,
      sendDiffOnly,
      extendLock,
      failureHandlingStrategy
    )(ctx => handle(ctx.variables))
  }


  def routeEither[
    F[_] : TopicBuilder : Sync,
    IN: Decoder,
    OUT: Encoder,
    BPMN_OUT: Encoder](topic: String,
                       differentLoop: Option[LoopSettings] = None,
                       sendDiffOnly: Boolean = true,
                       extendLock: Boolean = false,
                       failureHandlingStrategy: FailureHandlingStrategy = FailureHandlingStrategy.expRetries())
                      (handle: IN => F[Either[BpmnError[BPMN_OUT], OUT]]): Unit = {
    routeCtxEither[F, IN, OUT, BPMN_OUT](
      topic,
      differentLoop,
      sendDiffOnly,
      extendLock,
      failureHandlingStrategy
    )(ctx => handle(ctx.variables))
  }


  def routeCtx[
    F[_] : TopicBuilder : Sync,
    IN: Decoder,
    OUT: Encoder](topic: String,
                  differentLoop: Option[LoopSettings] = None,
                  sendDiffOnly: Boolean = true,
                  extendLock: Boolean = false,
                  failureHandlingStrategy: FailureHandlingStrategy = FailureHandlingStrategy.expRetries())
                 (handle: ExternalTask[IN] => F[OUT]): Unit = {
    routeCtxEither[F, IN, OUT, Json](
      topic,
      differentLoop,
      sendDiffOnly,
      extendLock,
      failureHandlingStrategy
    )(ctx => handle(ctx).map(Right.apply))
  }

  def routeCtxEither[
    F[_] : TopicBuilder : Sync,
    IN: Decoder,
    OUT: Encoder,
    BPMN_OUT: Encoder](topic: String,
                       differentLoop: Option[LoopSettings] = None,
                       sendDiffOnly: Boolean = true,
                       extendLock: Boolean = false,
                       failureHandlingStrategy: FailureHandlingStrategy = FailureHandlingStrategy.expRetries())
                      (handle: ExternalTask[IN] => F[Either[BpmnError[BPMN_OUT], OUT]]): Unit = {

    val handling: ExternalTask[Json] => F[Either[BpmnError[Json], Json]] = { task =>
      for {
        decodedJson <- Sync[F].fromEither(Decoder[IN].decodeJson(task.variables))
        decodedTask = task.copy(variables = decodedJson: IN)
        result <- handle(decodedTask)
        encodedResult = result match {
          case Left(bpmn) =>
            val json = Encoder[BPMN_OUT].apply(bpmn.variables)
            val encodedBpmn = bpmn.copy(variables = json)
            Left(encodedBpmn)
          case Right(result) =>
            val json = Encoder[OUT].apply(result)
            Right(json)
        }
      } yield encodedResult
    }


    val subscription = new Subscription[F](
      topic = topic,
      processDefinitionKeys = TopicBuilder[F].processDefKeys,
      handling = handling,
      differentLoop = differentLoop,
      sendDiffOnly = sendDiffOnly,
      extendLock = extendLock,
      failureHandlingStrategy = failureHandlingStrategy
    )
    TopicBuilder[F] += subscription
  }


}

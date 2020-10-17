package a14e.commons.camundadsl

import a14e.commons.camundadsl.TopicRoute.TopicBuilder
import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.traverse.TraverseImplicits._
import cats.Traverse
import cats.effect.{ConcurrentEffect, ContextShift, Effect, Sync, Timer}
import com.typesafe.scalalogging.LazyLogging
import org.camunda.bpm.client.ExternalTaskClient
import org.camunda.bpm.client.topic.TopicSubscription

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.higherKinds


class TopicRoute[F[_] : Sync](vector: List[CamundaSubscription[F]]) {

  import cats.implicits._

  def run(client: ExternalTaskClient): F[Seq[TopicSubscription]] = {
    Traverse[List].serially(vector) { subscription =>
      subscription.run(client)
    }.map(_.toSeq)
  }
}

object TopicRoute {
  def build[F[_] : Sync](processDefKey: String)
                        (builder: TopicBuilder[F] => Unit): TopicRoute[F] = {
    val topicBuilder = new TopicBuilder[F](processDefKey)
    builder(topicBuilder)
    topicBuilder.build
  }

  class TopicBuilder[F[_] : Sync](val processDefKey: String) {
    def +=(s: CamundaSubscription[F]): TopicBuilder[F] = {
      buffer += s
      this
    }

    def route[IN: RootDecoder, OUT: RootEncoder](topic: String,
                                                 lockDuration: FiniteDuration = 20.seconds,
                                                 errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                                                 bpmnErrors: Boolean = false)
                                                (handle: IN => F[OUT])
                                                (implicit
                                                 shift: ContextShift[F],
                                                 ef: Effect[F]): Unit = {
      implicit val builder: TopicBuilder[F] = this
      Routes.route[F, IN, OUT](topic, lockDuration, errorStrategy, bpmnErrors)(handle)
    }

    def routeWithLockUpdate[IN: RootDecoder, OUT: RootEncoder](topic: String,
                                                               lockDuration: FiniteDuration = 20.seconds,
                                                               timeStep: FiniteDuration = 15.seconds,
                                                               errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                                                               bpmnErrors: Boolean = false)
                                                              (handle: IN => F[OUT])
                                                              (implicit
                                                               coneffect: ConcurrentEffect[F],
                                                               shift: ContextShift[F],
                                                               timer: Timer[F]): Unit = {
      implicit val builder: TopicBuilder[F] = this
      Routes.routeWithLockUpdate[F, IN, OUT](topic, lockDuration, timeStep, errorStrategy, bpmnErrors)(handle)
    }

    def build: TopicRoute[F] = new TopicRoute[F](buffer.result())

    private val buffer = new ListBuffer[CamundaSubscription[F]]
  }

  object TopicBuilder {
    def apply[F[_] : TopicBuilder]: TopicBuilder[F] = implicitly[TopicBuilder[F]]
  }

}

object Routes extends LazyLogging {

  import cats.implicits._

  def route[
    F[_] : TopicBuilder : ContextShift : Effect,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      bpmnErrors: Boolean = false)
                     (handle: IN => F[OUT]): Unit = {
    routeCtx[F, IN, OUT](topic, lockDuration, errorStrategy, bpmnErrors)(ctx => handle(ctx.value))
  }

  def routeEither[
    F[_] : TopicBuilder : ContextShift : Effect,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries)
                     (handle: IN => F[Either[BpmnError, OUT]]): Unit = {
    routeCtxEither[F, IN, OUT](topic, lockDuration, errorStrategy)(ctx => handle(ctx.value))
  }

  def routeCtxEither[
    F[_] : TopicBuilder : ContextShift : Effect,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries)
                     (handle: CamundaContext[F, IN] => F[Either[BpmnError, OUT]]): Unit = {


    val subscription = new CamundaSubscriptionF[F, IN, OUT](
      processDefKey = TopicBuilder[F].processDefKey,
      topic = topic,
      lockDuration = lockDuration,
      errorStrategy = errorStrategy
    )(handle)
    TopicBuilder[F] += subscription
  }

  def routeCtx[
    F[_] : TopicBuilder : ContextShift : Effect,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      bpmnErrors: Boolean = false)
                     (handle: CamundaContext[F, IN] => F[OUT]): Unit = {
    routeCtxEither[F, IN, OUT](topic, lockDuration, errorStrategy) {
      ctx =>
        handle(ctx)
          .map(Either.right[BpmnError, OUT](_))
          .handleErrorWith(err =>
            if (bpmnErrors) Sync[F].delay(Left(BpmnError(err)))
            else Sync[F].raiseError(err)
          )
    }
  }


  def routeWithLockUpdate[F[_], IN: RootDecoder, OUT: RootEncoder](topic: String,
                                                                   lockDuration: FiniteDuration = 20.seconds,
                                                                   timeStep: FiniteDuration = 15.seconds,
                                                                   errorStrategy: ErrorStrategy = ErrorStrategy.failAndStop,
                                                                   bpmnErrors: Boolean = false)
                                                                  (handle: IN => F[OUT])
                                                                  (implicit
                                                                   coneffect: ConcurrentEffect[F],
                                                                   builder: TopicBuilder[F],
                                                                   shift: ContextShift[F],
                                                                   timer: Timer[F]): Unit = {
    import fs2.Stream
    routeCtx[F, IN, OUT](topic, lockDuration, errorStrategy, bpmnErrors) { ctx =>
      val handling = Stream.eval[F, OUT](handle(ctx.value))
      val prolongating = Stream.awakeEvery[F](timeStep)
        .evalMap(_ => ctx.service.extendLock(lockDuration)) // предложевается до времени = текущее время + lockDuration
        .filter(_ => false)
        .map(_.asInstanceOf[OUT])

      handling.mergeHaltBoth(prolongating)
        .compile
        .lastOrError
    }
  }


}

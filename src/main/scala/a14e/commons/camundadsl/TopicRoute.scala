package a14e.commons.camundadsl

import a14e.commons.camundadsl.TopicRoute.TopicBuilder
import a14e.commons.camundadsl.Types.CamundaContext
import a14e.commons.catseffect.CatsIoImplicits._
import a14e.commons.context.Contextual
import cats.arrow.FunctionK
import cats.{Traverse, ~>}
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Effect, IO, Sync, Timer}
import com.typesafe.scalalogging.LazyLogging
import org.camunda.bpm.client.ExternalTaskClient
import org.camunda.bpm.client.topic.TopicSubscription

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.higherKinds


class TopicRoute[F[_]](val routes: List[CamundaSubscription[F]]) extends LazyLogging {

  import cats.implicits._

  def ++(other: TopicRoute[F]): TopicRoute[F] = new TopicRoute[F](this.routes ++ other.routes)


  def to[B[_]](to: F ~> B)
              (implicit
               shift: ContextShift[F],
               effect: Sync[F],
               contextual: Contextual[F]): TopicRoute[B] = {
    new TopicRoute[B](routes.map(_.to(to)))
  }

  def runWithEffect(client: ExternalTaskClient)(implicit
                                                shift: ContextShift[F],
                                                effect: Effect[F],
                                                contextual: Contextual[F]): F[Seq[TopicSubscription]] = {
    run(client, effectRun(_, _))
  }

  type Topic = String

  def run(client: ExternalTaskClient,
          runner: (F[_], Topic) => Unit)(implicit
                                         shift: ContextShift[F],
                                         sync: Sync[F],
                                         contextual: Contextual[F]): F[Seq[TopicSubscription]] = {
    Sync[F].serially(routes) { subscription =>
      subscription.run(client, runner(_, subscription.topic))
    }.map(_.toSeq)
  }

  def runBy[B[_]](client: ExternalTaskClient)
                 (by: F[_] => B[_])(implicit
                                    shift: ContextShift[F],
                                    sync: Sync[F],
                                    contextual: Contextual[F],
                                    effectB: Effect[B],
                                    shiftB: ContextShift[B]): F[Seq[TopicSubscription]] = {
    run(client, (io, topic) => effectRun(by(io), topic))
  }

  private def effectRun[B[_] : Effect : ContextShift](ioToRun: B[_], topic: String): Unit = {
    // TODO тут можно подложить контекст из runBy
    Effect[B].runAsync(ioToRun) {
      case Left(err) =>
        IO.delay(logger.error(s"Handling of topic $topic failed with error", err))
      case Right(_) =>
        IO.delay(logger.info(s"Handling of topic $topic completed with success"))
    }.toIO
      .unsafeRunAsyncAndForget()
  }


}

object TopicRoute {
  def build[F[_]](processDefKey: String)
                 (builder: TopicBuilder[F] => Unit): TopicRoute[F] = {
    val topicBuilder = new TopicBuilder[F](processDefKey)
    builder(topicBuilder)
    topicBuilder.build
  }

  class TopicBuilder[F[_]](val processDefKey: String) {
    def +=(s: CamundaSubscription[F]): TopicBuilder[F] = {
      buffer += s
      this
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
    F[_] : TopicBuilder: Sync,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      bpmnErrors: Boolean = false,
                      sendDiffOnly: Boolean = true)
                     (handle: IN => F[OUT]): Unit = {
    routeCtx[F, IN, OUT](topic, lockDuration, errorStrategy, bpmnErrors, sendDiffOnly)(ctx => handle(ctx.value))
  }

  def routeEither[
    F[_] : TopicBuilder: Sync,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      sendDiffOnly: Boolean = true)
                     (handle: IN => F[Either[BpmnError, OUT]]): Unit = {
    routeCtxEither[F, IN, OUT](topic, lockDuration, errorStrategy, sendDiffOnly)(ctx => handle(ctx.value))
  }

  def routeCtxEither[
    F[_] : TopicBuilder,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      sendDiffOnly: Boolean = true)
                     (handle: CamundaContext[F, IN] => F[Either[BpmnError, OUT]]): Unit = {


    val subscription = new CamundaSubscriptionF[F, IN, OUT](
      processDefKey = TopicBuilder[F].processDefKey,
      topic = topic,
      lockDuration = lockDuration,
      errorStrategy = errorStrategy,
      sendDiffOnly = sendDiffOnly
    )(handle)
    TopicBuilder[F] += subscription
  }

  def routeCtx[
    F[_] : TopicBuilder: Sync,
    IN: RootDecoder,
    OUT: RootEncoder](topic: String,
                      lockDuration: FiniteDuration = 20.seconds,
                      errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                      bpmnErrors: Boolean = false,
                      sendDiffOnly: Boolean = true)
                     (handle: CamundaContext[F, IN] => F[OUT]): Unit = {
    routeCtxEither[F, IN, OUT](topic, lockDuration, errorStrategy, sendDiffOnly) {
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
                                                                   timeStep: FiniteDuration = 13.seconds,
                                                                   errorStrategy: ErrorStrategy = ErrorStrategy.simpleExpRetries,
                                                                   bpmnErrors: Boolean = false,
                                                                   sendDiffOnly: Boolean = true)
                                                                  (handle: IN => F[OUT])
                                                                  (implicit
                                                                   concurrent: Concurrent[F],
                                                                   builder: TopicBuilder[F],
                                                                   timer: Timer[F]): Unit = {
    import fs2.Stream
    routeCtx[F, IN, OUT](topic, lockDuration, errorStrategy, bpmnErrors, sendDiffOnly) { ctx =>
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

package a14e.commons.camunda.subscription

import java.time.Duration

import a14e.commons.camunda.client.CamundaProtocol.ExternalTask
import a14e.commons.camunda.configuration.LoopSettings
import cats.effect.Sync
import io.circe.Json


case class BpmnError[T](errorCode: String,
                        errorMessage: String,
                        variables: T)

case class Subscription[F[_]](topic: String,
                              processDefinitionKeys: Seq[String],
                              handling: ExternalTask[Json] => F[Either[BpmnError[Json], Json]],
                              differentLoop: Option[LoopSettings],
                              sendDiffOnly: Boolean, // если true -- мы обновляем по переменным только изменившиеся значения
                              extendLock: Boolean, // ессли true -- мы продлеваем локи по данной таске
                              failureHandlingStrategy: FailureHandlingStrategy)

object Subscription {
  type OUT = Either[BpmnError[Json], Json]
}


trait Filter[F[_]] {
  self =>
  import Subscription.OUT

  def filter(task: ExternalTask[Json],
             handling: ExternalTask[Json] => F[OUT], // тут могут быть обработчики из предыдущих фильтров
             subscribtion: Subscription[F]): F[OUT]

  def merge(other: Filter[F]): Filter[F] =
    (task: ExternalTask[Json], handling: ExternalTask[Json] => F[OUT], subscribtion: Subscription[F]) =>
      // более ранний -- более глобальный
      self.filter(task, other.filter(_, handling, subscribtion), subscribtion)


}

object Filter {
  import Subscription.OUT
  def pure[F[_]]: Filter[F] = {
    (task: ExternalTask[Json], handling: ExternalTask[Json] => F[OUT], _: Subscription[F]) => handling(task)
  }
}

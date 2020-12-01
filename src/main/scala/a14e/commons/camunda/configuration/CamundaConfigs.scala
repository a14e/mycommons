package a14e.commons.camunda.configuration

import java.time.Duration

import com.typesafe.config.Config
import pureconfig.ConfigSource

case class CamundaConfigs(baseUrl: String,
                          defaultLoop: LoopSettings)


object CamundaConfigs {
  def from(config: Config, pathPrefix: String = "camunda"): CamundaConfigs = {
    import pureconfig.generic.auto._
    ConfigSource.fromConfig(config)
      .at(pathPrefix)
      .loadOrThrow[CamundaConfigs]
  }
}


case class LoopSettings(fetchSize: Int, // размер батчи, которую берем для обработки
                        parallelLevel: Int, // параллельность обработки процесса
                        lockTimeout: Duration, // время, на которое берем лок процесса
                        extendLockTimeout: Duration, // шаг, с которым продлеваем долгие задачи
                        asyncResponseTimeout: Duration, // интервал ожидания для Long Poling
                        errorRestartTimeout: Duration, // в случае ошибок через такой интервал перезапускаем Loop
                        lockSafetyTimeout: Duration, // запас по времени лока,  если у нас лок истекает через lockSafetyTimeout -- не начинаем исполнять таску
                        usePriority: Boolean)


object LoopSettings {

  import a14e.commons.time.TimeImplicits._

  val default = LoopSettings(
    fetchSize = 6,
    parallelLevel = 6,
    lockTimeout = 20.seconds,
    extendLockTimeout = 15.seconds,
    asyncResponseTimeout = 20.seconds,
    errorRestartTimeout = 20.seconds,
    lockSafetyTimeout = 1.seconds,
    usePriority = true
  )

  val slowTasksLoop = default.copy(
    fetchSize = 3,
    parallelLevel = 8
  )

}

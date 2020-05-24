package a14e.commons.services

import com.softwaremill.macwire._

trait UtilsModule {

  lazy val currentRunService: CurrentRunService = wire[CurrentRunServiceImpl]
  lazy val timeService: TimeService = wire[TimeServiceImpl]
  lazy val randomGeneratingService: RandomGeneratingService = wire[RandomGeneratingServiceImpl]
}



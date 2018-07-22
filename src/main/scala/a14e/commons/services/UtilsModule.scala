package a14e.commons.services

trait UtilsModule {

  lazy val timeServiceImpl: TimeService = new TimeServiceImpl
  lazy val idServiceImpl: RandomGeneratingService = new RandomGeneratingServiceImpl
}



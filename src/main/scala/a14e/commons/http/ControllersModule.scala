package a14e.commons.http

import a14e.commons.controller.Controller


trait ControllersModule {

  def controllers: Seq[Controller]

  def afterRejectControllers: Seq[Controller]

}

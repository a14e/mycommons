package a14e.utils.http

import a14e.utils.controller.Controller


trait ControllersModule {

  def controllers: Seq[Controller]

  def afterRejectControllers: Seq[Controller]

}

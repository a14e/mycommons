package a14e.commons.concurrent

import scala.concurrent.ExecutionContext


object FutureUtils {
  lazy val sameThreadExecutionContext: ExecutionContext = new ExecutionContext {
    override def execute(runnable: Runnable): Unit = runnable.run()

    override def reportFailure(cause: Throwable): Unit = ()
  }
}
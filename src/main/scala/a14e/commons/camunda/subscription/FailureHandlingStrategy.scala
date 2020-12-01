package a14e.commons.camunda.subscription

import java.time.Duration


trait FailureHandlingStrategy {


  def nextTimeout(retry: Int): Option[Duration]

  def maxRetries: Int
}

object FailureHandlingStrategy {

  import a14e.commons.time.TimeImplicits._

  val noRetries: FailureHandlingStrategy = new FailureHandlingStrategy {
    override def nextTimeout(retry: Int): Option[Duration] = None

    override def maxRetries: Int = 0
  }

  def constRetries(maxRetriesCount: Int = 10,
                   step: Duration = 20.seconds): FailureHandlingStrategy = new FailureHandlingStrategy {
    override def nextTimeout(retry: Int): Option[Duration] = {
      if (retry > maxRetriesCount) None
      else Some(step)
    }

    override def maxRetries: Int = maxRetriesCount
  }

  def expRetries(maxRetriesCount: Int = 10,
                 seed: Int = 4,
                 init: Duration = 20.seconds,
                 max: Duration = 20.minutes): FailureHandlingStrategy = new FailureHandlingStrategy {
    override def nextTimeout(retry: Int): Option[Duration] = {
      if (retry > maxRetriesCount) None
      else {
        val nextMillis = (init.toMillis * math.pow(seed, retry)).toLong
        if (nextMillis < 0 || nextMillis > max.toMillis) Some(max)
        else Some(nextMillis.millis)
      }
    }

    override def maxRetries: Int = maxRetriesCount
  }


}

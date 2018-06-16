package a14e.utils.services

import java.util.UUID

trait CurrentRunService {

  def runId: String
}

class CurrentRunServiceImpl extends CurrentRunService {

  def runId: String = RunServiceState.runId
}

object RunServiceState {
  final val runId: String = UUID.randomUUID().toString
}
val checkStatusScheduler = context.system.scheduler.schedule(batchFrequency, batchFrequency, self, CheckBatchStatus)

override def postStop(): Unit = {
  checkStatusScheduler.cancel()
}

def receive = {
  case id: Int =>
    batch(sender(), id)
  case CheckBatchStatus =>
    if(requestDone) requestDone = false
    else makeRequest()
}

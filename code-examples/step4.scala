def makeRequest() = {
  if (concurrentCalls == maxConcurrentCalls)
    log.warning(s"Concurrent call limit ($maxConcurrentCalls) has been reached.")
  else {
    requestDone = true
    if (!ids.isEmpty) {
      concurrentCalls += 1
      log.info(s"Concurrent calls: $concurrentCalls")
      val split = ids.splitAt(batchSize)
      val copy = split._1
      ids = split._2
      httpRequest(copy.values.mkString("-")).onComplete {
        case Success(result) =>
          copy.foreach {
            case (actorRef, id) =>
              actorRef ! result
          }
          concurrentCalls -= 1
          log.info(s"Concurrent calls: $concurrentCalls"  )
        case Failure(f) =>
          log.error(s"Coll to service failed: $f")
          concurrentCalls -= 1
      }
    }
  }
}

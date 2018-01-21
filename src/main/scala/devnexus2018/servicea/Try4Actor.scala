package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props}

import scala.collection.immutable.Map
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Problem with Try3:
  * In the case of a massive amount of simultaneous calls service B will be flooded and the SLA is not met (again.)
  * Test: "ab -n 500 -c 100 http://127.0.0.1:8080/servicea/12"
  *
  * Solution in Try4:
  * Limit the number of simultaneous calls to service B.
  */
class Try4Actor extends BaseActor {
  import Try4Actor._
  val batchSize = context.system.settings.config.getInt("service-a.batch-size")
  val batchFrequency = FiniteDuration(context.system.settings.config.getDuration("service-a.batch-frequency").toMillis, TimeUnit.MILLISECONDS)
  val maxConcurrentCalls = context.system.settings.config.getInt("service-a.max-concurrent-calls")
  var ids = Map.empty[ActorRef, Int]
  val checkStatusScheduler = context.system.scheduler.schedule(batchFrequency, batchFrequency, self, CheckBatchStatus)
  var requestDone = false
  var concurrentCalls = 0

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

  def batch(sender: ActorRef, id: Int) = {
    ids += sender -> id
    if (ids.size == batchSize) makeRequest()
  }

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
}

object Try4Actor {
  def props: Props = Props[Try4Actor]
  case object CheckBatchStatus
}

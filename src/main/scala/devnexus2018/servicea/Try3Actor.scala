package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Cancellable, Props}

import scala.collection.immutable.Map
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

/**
  * Problem with Try2:
  * If the number of requests fluctuate, i.e. if it does not match our batch size, it will cause some requests to time out.
  *
  * Solution in Try3:
  * Create a scheduled call to service B to handle any lingering requests in the internal map.
  * Furthermore, prevent that calls happen more frequently than 1 second (i.e. if requests have just been requestDone it should wait at least the batch frequency time before sending again.)
  */
class Try3Actor extends BaseActor {
  import Try3Actor._
  val batchSize = context.system.settings.config.getInt("service-a.batch-size")
  val batchFrequency = FiniteDuration(context.system.settings.config.getDuration("service-a.batch-frequency").toMillis, TimeUnit.MILLISECONDS)
  var ids = Map.empty[ActorRef, Int]
  val checkStatusScheduler = context.system.scheduler.schedule(batchFrequency, batchFrequency, self, CheckBatchStatus)
  var requestDone = false

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
    requestDone = true
    if (!ids.isEmpty) {
      val copy = ids
      ids = Map.empty[ActorRef, Int]
      httpRequest(copy.values.mkString("-")).onComplete {
        case Success(result) =>
          copy.foreach {
            case (actorRef, id) =>
              actorRef ! result
          }
        case Failure(f) =>
          log.error(s"Coll to service failed: $f")
      }
    }
  }
}

object Try3Actor {
  def props: Props = Props[Try3Actor]
  case object CheckBatchStatus
}
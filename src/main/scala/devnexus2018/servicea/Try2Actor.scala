package devnexus2018.servicea

import akka.actor.{ActorRef, Props}

import scala.collection.immutable.Map
import scala.util.{Failure, Success}

/**
  * Problem with Try1:
  * Too many individual calls leads to a violation of the SLA with service B.
  *
  * Solution in Try2:
  * Implement batching of X calls before calling the service B.
  */
class Try2Actor extends BaseActor {
  val batchSize = context.system.settings.config.getInt("service-a.batch-size")
  var ids = Map.empty[ActorRef, Int]

  def receive = {
    case id: Int => batch(sender(), id)
  }

  def batch(sender: ActorRef, id: Int) = {
    ids += sender -> id
    if (ids.size == batchSize) {
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

object Try2Actor {
  def props: Props = Props[Try2Actor]
}

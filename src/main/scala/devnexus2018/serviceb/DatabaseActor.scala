package devnexus2018.serviceb

import akka.actor.{Actor, ActorLogging, Props}

class DatabaseActor extends Actor with ActorLogging {
  def receive = {
    case identifiers: String =>
      var totalTime = 0
      identifiers.split("-").map(_.toInt).foreach { id =>
        Thread.sleep(id)
        totalTime += id
      }
      sender ! s"TOTAL DB TIME: $totalTime"
  }
}

object DatabaseActor {
  def props: Props = Props[DatabaseActor]
}

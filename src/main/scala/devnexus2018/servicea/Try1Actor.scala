package devnexus2018.servicea

import akka.actor.Props

import scala.util.Success

/**
  * Basic, straight forward, implementation that wires service A and B together.
  */
class Try1Actor extends BaseActor {
  def receive: Receive = {
    case id: Int =>
      val s = sender()
      httpRequest(id.toString).onComplete {
        case Success(result) => s ! result
        case _ =>
      }
  }
}

object Try1Actor {
  def props: Props = Props[Try1Actor]
}
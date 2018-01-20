package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Try1Actor extends Actor with ActorLogging {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  val http = Http(context.system)
  val uri = context.system.settings.config.getString("service-b")
  def receive: Receive = {
    case id: Int =>
      val s = sender
      http.singleRequest(HttpRequest(uri = Uri(s"$uri/$id"))).onComplete {
        case Success(response) =>
          if (response.status == StatusCodes.OK)
            response.entity.toStrict(FiniteDuration(5000, TimeUnit.MILLISECONDS)).map(_.data.utf8String).onComplete {
              case Success(data) => s ! data
              case _ =>
            }
          else
            log.error(s"Service returned status: ${response.status} for URI: $uri/$id")
        case Failure(f) => log.error(s"Could not call service: $uri. Reason: $f")
      }
  }
}

object Try1Actor {
  def props: Props = Props[Try1Actor]
}
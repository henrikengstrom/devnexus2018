package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

abstract class BaseActor extends Actor with ActorLogging {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  val http = Http(context.system)
  val uri = context.system.settings.config.getString("service-a.service-b-uri")

  def httpRequest(payload: String): Future[String] = {
    val promise = Promise[String]()

    http.singleRequest(HttpRequest(uri = Uri(s"$uri/$payload"))).onComplete {
      case Success(response) =>
        if (response.status == StatusCodes.OK)
          promise.completeWith(response.entity.toStrict(FiniteDuration(5000, TimeUnit.MILLISECONDS)).map(_.data.utf8String))
        else {
          log.error(s"Service sreturned status: ${response.status} for URI: $uri/$payload")
          promise.failure(new RuntimeException(s"Unexpected Status Code: ${response.status}"))
        }
      case Failure(f) =>
        log.error(s"Could not call service: $uri. Reason: $f")
        promise.failure(f)
    }

    promise.future
  }
}

package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.dispatch.Futures
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

abstract class BaseActor extends Actor with ActorLogging {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  val http = Http(context.system)
  val uri = context.system.settings.config.getString("service-a.service-b-uri")

  def httpRequest(requestPayload: String): Future[String] = {
    val promise = Promise[String]()
    callService(requestPayload,
      (response: HttpResponse) => {
        if (response.status == StatusCodes.OK) promise.completeWith(convertResponse(response))
        else {
          log.error(s"Service sreturned status: ${response.status} for URI: $uri/$requestPayload")
          promise.failure(new RuntimeException(s"Unexpected HTTP status code: ${response.status}"))
        }
      },
      (f: Throwable) => promise.failure(f)
    )
    promise.future
  }

  def httpRequest(requestPayload: String, pendingResponses: Seq[Promise[HttpResponse]]): Future[String] = {
    callService(requestPayload,
      (response: HttpResponse) => {
        if (response.status == StatusCodes.OK) {
          val result = Await.result(convertResponse(response), timeout.duration)
          val output = HttpResponse(entity = HttpEntity(result))
          pendingResponses.foreach(_.complete(Try { output } ))
        }
        else {
          log.error(s"Service sreturned status: ${response.status} for URI: $uri/$requestPayload")
          val f = new RuntimeException(s"Unexpected HTTP status code: ${response.status}")
          pendingResponses.foreach(_.failure(f))
        }

      },
      (f: Throwable) => pendingResponses.foreach(_.failure(f))
    )
    Futures.successful(requestPayload) // reply with whatever since the returned value is not used by the stream
  }

  private def convertResponse(response: HttpResponse): Future[String] =
    response.entity.toStrict(FiniteDuration(5000, TimeUnit.MILLISECONDS)).map(_.data.utf8String)

  private def callService(requestPayload: String, success: HttpResponse => Unit, failure: Throwable=> Unit): Unit = {
    http.singleRequest(HttpRequest(uri = Uri(s"$uri/$requestPayload"))).onComplete {
      case Success(response) => success(response)
      case Failure(f) =>
        log.error(s"Could not call service: $uri. Reason: $f")
        failure(f)
    }
  }
}

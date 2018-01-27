package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import devnexus2018.servicea.StreamActor.Request

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.StdIn

object StreamMain {
  implicit val system: ActorSystem = ActorSystem("ServiceA")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  // Needed for the Futures used
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  def main(args: Array[String]): Unit = {
    startServer()
  }

  private def startServer() = {
    val streamActor = system.actorOf(StreamActor.props, "streamActor")
    val serverBindingFuture: Future[ServerBinding] = Http().bindAndHandle(createRoute(streamActor), "localhost", 8080)

    println(s"Stream server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    serverBindingFuture
      .flatMap(_.unbind())
      .onComplete { done =>
        done.failed.map { e => println(s"Failed unbinding: $e") }
        system.terminate()
      }

  }

  private def createRoute(streamActor: ActorRef): Route = {
    pathPrefix("servicea" / IntNumber) { id =>
      get {
        val httpResponsePromise = Promise[HttpResponse]()
        streamActor ! Request(id, httpResponsePromise)
        complete(httpResponsePromise.future)
      }
    }
  }
}

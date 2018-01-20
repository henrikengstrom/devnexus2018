package devnexus2018.serviceb

import java.util.concurrent.TimeUnit

import akka.actor.Status.Success
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.util.Failure

object BMain {
  implicit val system: ActorSystem = ActorSystem("ServiceB")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
  val dbActor: ActorRef = system.actorOf(DatabaseActor.props, "dbActor")

  def main(args: Array[String]): Unit = {
    startServer()
  }

  def startServer() = {
    val serverBindingFuture: Future[ServerBinding] = Http().bindAndHandle(createRoute(), "localhost", 8081)

    println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")
    StdIn.readLine()
    serverBindingFuture
      .flatMap(_.unbind())
      .onComplete { done =>
        done.failed.map { e => println(s"Failed unbinding: $e") }
        system.terminate()
      }
  }

  def createRoute(): Route = {
    pathPrefix("serviceb" / Segment) { ids =>
      get {
        complete((dbActor ? ids).mapTo[String])
      }
    }
  }
}

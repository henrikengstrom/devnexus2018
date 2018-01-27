package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.pattern.ask
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.StdIn

object ActorMain {
  implicit val system: ActorSystem = ActorSystem("ServiceA")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  def main(args: Array[String]): Unit = {
    val backendActor: Option[ActorRef] =
      if (args.length != 1) {
        println("Service A requires a parameter.")
        None
      } else {
        args(0).toInt match {
          case 1 => Some(system.actorOf(Try1Actor.props, "try1"))
          case 2 => Some(system.actorOf(Try2Actor.props, "try2"))
          case 3 => Some(system.actorOf(Try3Actor.props, "try3"))
          case 4 => Some(system.actorOf(Try4Actor.props, "try4"))
          case _ =>
            println("Parameter must be between 1-4.")
            None
        }
      }

    startServer(backendActor)
  }

  def startServer(backendActor: Option[ActorRef]) = {
    if (backendActor.isDefined) {
      val serverBindingFuture: Future[ServerBinding] = Http().bindAndHandle(createRoute(backendActor.get), "localhost", 8080)

      println(s"Actor server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine()
      serverBindingFuture
        .flatMap(_.unbind())
        .onComplete { done =>
          done.failed.map { e => println(s"Failed unbinding: $e") }
          system.terminate()
        }
    } else {
      system.terminate()
    }
  }

  def createRoute(actor: ActorRef): Route = {
    pathPrefix("servicea" / IntNumber) { id =>
      get {
        val result = (actor ? id).mapTo[String]
        complete(result)
      }
    }
  }
}

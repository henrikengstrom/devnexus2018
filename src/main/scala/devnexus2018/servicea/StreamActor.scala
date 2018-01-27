package devnexus2018.servicea

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{MergeHub, Sink, Source}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration

object StreamActor {
  def props = Props[StreamActor]

  case class Request(id: Int, responsePromise: Promise[HttpResponse])
}

class StreamActor extends BaseActor {
  import StreamActor._

  val batchSize = context.system.settings.config.getInt("service-a.batch-size")
  val batchFrequency = FiniteDuration(context.system.settings.config.getDuration("service-a.batch-frequency").toMillis, TimeUnit.MILLISECONDS)
  val maxConcurrentCalls = context.system.settings.config.getInt("service-a.max-concurrent-calls")

  val sink =
    MergeHub.source[Request]
        .groupedWithin(batchSize, batchFrequency)
          .map(requests => (requests.map(req => req.id).mkString("-"), requests.map(_.responsePromise)))
            .mapAsync(maxConcurrentCalls)(payload => httpRequest(payload._1, payload._2))
              .to(Sink.ignore)
                .run()

  def receive = {
    case req: Request => Source.single(req).runWith(sink)
  }
}

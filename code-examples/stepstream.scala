case class Request(id: Int, responsePromise: Promise[HttpResponse])

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

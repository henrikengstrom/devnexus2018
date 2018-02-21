val batchSize = context.system.settings.config.getInt("service-a.batch-size")
var ids = Map.empty[ActorRef, Int]

case id: Int => batch(sender(), id)

def batch(sender: ActorRef, id: Int) = {
  ids += sender -> id
  if (ids.size == batchSize) {
    val copy = ids
    ids = Map.empty[ActorRef, Int]
    httpRequest(copy.values.mkString("-")).onComplete {
      case Success(result) =>
        copy.foreach {
          case (actorRef, id) =>
            actorRef ! result
        }
      case Failure(f) =>
        log.error(s"Coll to service failed: $f")
    }
  }
}

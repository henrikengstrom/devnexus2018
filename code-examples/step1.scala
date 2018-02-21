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

def foreach[T](f: T ⇒ Unit): Sink[T, Future[Done]]

def ignore: Sink[Any, Future[Done]]

def head[T]: Sink[T, Future[T]]

def actorRef[T](ref: ActorRef, onCompleteMessage: Any): Sink[T, NotUsed]

def last[T]: Sink[T, Future[T]]

def seq[T]: Sink[T, Future[immutable.Seq[T]]]

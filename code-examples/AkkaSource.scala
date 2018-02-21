def fromIterator[T](f: () ⇒ Iterator[T]): Source[T, NotUsed]

def fromFuture[T](future: Future[T]): Source[T, NotUsed]

def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Cancellable]

def single[T](element: T): Source[T, NotUsed]

def repeat[T](element: T): Source[T, NotUsed]

def empty[T]: Source[T, NotUsed]

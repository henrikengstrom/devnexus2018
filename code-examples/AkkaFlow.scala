def drop(n: Long): Repr[Out]

def dropWithin(d: FiniteDuration): Repr[Out]

def filter(p: Out ⇒ Boolean): Repr[Out]

def filterNot(p: Out ⇒ Boolean): Repr[Out]

def groupedWithin(n: Int, d: FiniteDuration): Repr[immutable.Seq[Out]]

def initialDelay(delay: FiniteDuration): Repr[Out]

def map[T](f: Out ⇒ T): Repr[T]

def mapAsync[T](parallelism: Int)(f: Out ⇒ Future[T]): Repr[T]

def takeWhile(p: Out ⇒ Boolean): Repr[Out]

def zip[U](that: Graph[SourceShape[U], _]): Repr[(Out, U)]

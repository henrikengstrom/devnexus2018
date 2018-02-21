Source.range(0, 10000)
  .filter(_ % 2 == 0)
  .map(._toString)
  .runForeach(n => println(s"Even number is: $n"))
  
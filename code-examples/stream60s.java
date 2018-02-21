final ActorSystem system = ActorSystem.create("StreamsTest");
final ActorMaterialization mat = ActorMaterializer.create(system);
final Source<Integer, NotUsed> source = Source.range(1, 10000);
final Flow<Integer, String, NotUsed> flow =
Flow.of(Integer.class).filter(n -> n % 2 == 0)
.map(e -> e.toString());
final Sink<String, CompletionStage<Done>> sink = 
Sink.foreach(s -> System.out.println(s));
final RunnableGraph<NotUsed> runnable = source.via(flow).to(sink);
runnable.run(); 

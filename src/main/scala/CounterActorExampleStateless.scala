import akka.actor.{Actor, ActorSystem, Props}

object CounterActorExampleStateless extends App {

  // DOMAIN of the counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }
  class Counter extends Actor {
    import Counter._ // import everything
    override def receive: Receive = counterReceive(0)
    def counterReceive(count: Int): Receive = {
      case Increment => context.become(counterReceive(count + 1))
      case Decrement => context.become(counterReceive(count - 1))
      case Print => println(s"Count ${count}")
    }
  }

  val system = ActorSystem("Example");

  val counter = system.actorOf(Props[Counter], "counterExample");

  // race conditions?
  // Only one thread operates on an actor at any time
  // --> actors are effectively single-threaded
  // --> no locks needed

  // Message delivery
  // "At most once delivery" guarantees ( no duplicates )

  // Message Order between two Actors are guaranteed ( the third actor's doesn't get guaranteed)
  // ex) Bob gave A to Alice, Bob gave B to Alice ---> A will arrive before B
  ( 1 to 5).foreach(_ => counter ! Counter.Increment)
  ( 1 to 2).foreach(_ => counter ! Counter.Decrement)
  counter ! Counter.Print

}

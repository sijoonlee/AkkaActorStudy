import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {
  val actorSystem = ActorSystem("firstActorSystem"); // do not use space, -, etc for name
  println(actorSystem.name);

  // Actors are uniquely identified
  // Messages are asynchronous
  // Each actor may respond differently
  // Actors are (really) encapsulated

  // Word count actor
  class WordCountActor extends Actor {
    var totalWords = 0;
    def receive: Receive /*PartialFunction[Any, Unit] */= { // PartialFunction[Any, Unit] is the same with Receive
      case message: String => totalWords += message.split(" ").length; println(s"I've got ${this.totalWords} words");
      case msg => println(s"[word count] can't understand ${msg.toString}")
    }
  }

  // We have to instantiate Actor via actor system
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter");
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter");

  // Communicate though reference - it's asynchronous
  wordCounter ! "I am learning akka" // or wordCounter.!("...")
  anotherWordCounter ! "I am learning akka !!!" // ! method is also known as "tell"

  // Best practice to provide initial value for class
  object Person {
    def props(name: String) = Props(new Person(name))
  }
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hello, I am ${name}")
      case _ =>
    }
  }

  val person = actorSystem.actorOf(Person.props("bob"));
  person ! "hi"
}


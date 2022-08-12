import akka.actor.{Actor, ActorLogging, ActorSystem, Kill, PoisonPill, Props}

object StoppingActorsSpecialMessage extends App {
  class MyActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("StoppingActorDemo");
  // use Special Message "PoisonPill"
  val looseActor = system.actorOf(Props[MyActor]);

  looseActor ! "Hello?"
  looseActor ! PoisonPill // Stop Actor
  looseActor ! "Hello????" // not delivered

  val terminatedActor = system.actorOf(Props[MyActor]);
  terminatedActor ! "will be terminated"
  terminatedActor ! Kill // [akka://StoppingActorDemo/user/$b] Kill (akka.actor.ActorKilledException: Kill)
  terminatedActor ! "terminated" // not delivered

}

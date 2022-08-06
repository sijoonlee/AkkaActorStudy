import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapability extends App {

  class SimpleActor extends Actor {

    override def receive: Receive = {
      // context.self is reference to self including identifier ex) Actor[akka://ActorCapabilitiesDemo/user/simpleActor#558828469]
      // context.self.path ex) akka://ActorCapabilitiesDemo/user/simpleActor
      // context.self is equal to self
      case "Hi!" =>  println(s"${self.path} I've got message 'Hi' from ${context.sender().path}- Hello There!"); context.sender() ! "Hello There"
      case message: String => println(s"[${context.self}] I've got message: ${message}")
      case number: Int => println(s"[${self.path}] I've got number: ${number}")
      case SpecialMessage(contents) => println(s"[SimpleActor] I've got special message: ${contents}")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => println(s"${self.path} say Hi to ${ref.path}"); ref ! "Hi!";
      case WirelessPhoneMessage(content, ref) => ref forward content // I keep the original sender
    }
  }

  val system = ActorSystem("ActorCapabilitiesDemo");
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor");

  simpleActor ! "Hello?"
  simpleActor ! 123 // messages can be of any type
  simpleActor ! SpecialMessage("I am special")

  // NOTE - message must be IMMUTABLE
  // NOTE - message must be SERIALIZABLE
  // in practice, use case classes and case objects

  case class SpecialMessage(contents: String)

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("Talk to myself")
  // Actors have information about their context and about themselves
  // context.self is equivalent to the keyword "this" in OOP

  val alice = system.actorOf(Props[SimpleActor], "alice");
  val bob = system.actorOf(Props[SimpleActor], "bob");

  case class SayHiTo(actorRef: ActorRef)

  alice ! SayHiTo(bob)

  // akka://ActorCapabilitiesDemo/user/alice I've got message 'Hi' from akka://ActorCapabilitiesDemo/deadLetters- Hello There!
  // Message [java.lang.String] from Actor[akka://ActorCapabilitiesDemo/user/alice#6775415]
  // to Actor[akka://ActorCapabilitiesDemo/deadLetters] was not delivered
  alice ! "Hi!"

  // Forwarding message
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi!", bob); // original sender = no sender
}


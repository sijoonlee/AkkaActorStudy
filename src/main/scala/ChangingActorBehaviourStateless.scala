import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviourStateless extends App {

  object FussyKid {
    case object Accept
    case object Reject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false) // true to discard old, false to stack handler
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! Accept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false) // default is true to avoid memory leak
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! Reject
    }
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEGETABLE = "vege"
    val CHOCOLATE = "choco"
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to go out?")
      case Accept => println("oh my kid is happy")
      case Reject => println("oh my kid is sad")
    }
  }

  val system = ActorSystem("example")

  val mom = system.actorOf(Props[Mom])
  val kid = system.actorOf(Props[StatelessFussyKid])

  mom ! Mom.MomStart(kid)

}

import ChildActors.CreditCard.{AttachToAccount, CheckStatus}
import ChildActors.Parent.CreateChild
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) =>
        if (childRef != null) childRef forward message
    }
  }

  class Child extends Actor {

    override def receive: Receive = {
      case message => println(s"${self.path} I got $message")
    }
  }

  import Parent._
  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("You are grounded")

  // Actor hierarchies
  // parent -> child -> grand child
  //           child

  // Guardian actors (top-level)
  // - /system = system guardian
  // - /user = user-level guardian
  // - / = the root guardian

  /**
   * Actor selection - use this when you want actor in deeper level of hierarchies
   */
  val childSelection = system.actorSelection("/user/parent/child")
  childSelection ! "I found you"

  /**
   * Danger !
   *
   * Never Pass Mutable Actor State, or The "This" Reference, To Child Actors
   */

  object NativeBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }
  class NativeBankAccount extends Actor {
    import NativeBankAccount._
    import CreditCard._

    var amount = 0;
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // Wrong! concurrency issue
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }
    def deposit(funds: Int) = {
      println(s"Current: ${amount} - add ${funds}")
      amount += funds;
    }
    def withdraw(funds: Int) = {
      println(s"Current: ${amount} - withdraw ${funds}")
      amount -= funds
    };
  }
  object CreditCard {
    case class AttachToAccount(bankAccount: NativeBankAccount) // ?
    case object CheckStatus
  }
  class CreditCard extends Actor {
    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachedTo(account))
    }
    def attachedTo(account: ChildActors.NativeBankAccount): Receive ={
      case CheckStatus => println(s"${self.path} your message got processed")
      account.withdraw(1) // WRONG! will mutate state without going though message
    }
  }

  import NativeBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NativeBankAccount], "account")

  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(100)
  val ccSelection = system.actorSelection("/user")
  ccSelection ! CheckStatus
}

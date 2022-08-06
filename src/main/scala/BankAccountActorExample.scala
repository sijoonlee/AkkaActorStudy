import BankAccountActorExample.Person.LiveTheLife
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object BankAccountActorExample extends App {

  val system = ActorSystem("Example");

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement
    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }
  class BankAccount extends Actor {
    import BankAccount._
    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Invalid deposit amount")
        else {
          funds += amount
          sender() ! TransactionSuccess("Deposit Success")
        }
      case Withdraw(amount) =>
        if (amount > funds) sender() ! TransactionFailure("Invalid withdraw amount")
        else {
          funds -= amount
          sender() ! TransactionSuccess("Withdraw Success")
        }
      case Statement => sender() ! s"Your balance ${funds}"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor {
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(3000)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "rich")

  person ! LiveTheLife(account)
}

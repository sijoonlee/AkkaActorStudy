import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisorSpec extends TestKit(ActorSystem("SupervisorSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisorSpec._
  "A supervisor" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "Hey I am here"
      child ! Report
      expectMsg(4)
    }
    "too big sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "A"
      child ! "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16" // Resume
      child ! Report
      expectMsg(1)
    }
    "empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "A"
      child ! "" // Restart
      child ! Report
      expectMsg(0)
    }
    "lower case" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)

      child ! "a" // Stop
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "number" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)

      child ! 1 // Escalate
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }
}

object SupervisorSpec {
  class Supervisor extends Actor with ActorLogging {
    override val supervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => log.info("Restart"); Restart
      case _: IllegalArgumentException => log.info("Stop"); Stop
      case _: RuntimeException => log.info("Resume"); Resume
      case _: Exception => log.info("Escalate"); Escalate // throw
    }

    override val receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  case object Report
  class FussyWordCounter extends Actor with ActorLogging {
    var words = 0
    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Empty sentence")
      case sentence: String =>
        if (sentence.length > 10) throw new RuntimeException("too bing sentence")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("it doesn't start with Upper case")
        else log.info("Pass"); words += sentence.split(" ").length
      case _ => throw new Exception("Should send String")
    }
  }
}

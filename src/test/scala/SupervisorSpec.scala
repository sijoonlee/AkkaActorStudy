import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
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

  "kinder supervisor" should {
    "not kill on restart or on escalate" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "A"
      child ! "" // restart
      child ! Report
      expectMsg(0)
      child ! 1 // escalate
      child ! Report
      expectMsg(0)
    }
  }

  "all-for-one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSuperVisor])
      supervisor ! Props[FussyWordCounter]
      val firstChild = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]
      secondChild ! "My testing"
      secondChild ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        firstChild ! "" // this will make the second child restart
      }

      Thread.sleep(500)

      secondChild ! Report
      expectMsg(0) // 0 because it restarted
    }
  }
}

object SupervisorSpec {
  class Supervisor extends Actor with ActorLogging {
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
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

  class NoDeathOnRestartSupervisor extends Supervisor {
    // original method
    //    def preRestart(@unused reason: Throwable, @unused message: Option[Any]): Unit = {
    //      context.children.foreach { child =>
    //        context.unwatch(child)
    //        context.stop(child)
    //      }
    //      postStop()
    //    }
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSuperVisor extends Supervisor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => log.info("Restart"); Restart
      case _: IllegalArgumentException => log.info("Stop"); Stop
      case _: RuntimeException => log.info("Resume"); Resume
      case _: Exception => log.info("Escalate"); Escalate // throw
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

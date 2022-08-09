import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)

    import TestProbSpec._

    "A Parent Actor" should {
      "register a child" in {
        val parent = system.actorOf(Props[Parent])
        val child = TestProbe("child")

        parent ! Register(child.ref)
        expectMsg(RegisterAck)
      }
    }

    "send the work to the child actor" in {
      val parent  = system.actorOf(Props[Parent])
      val child = TestProbe("child")
      parent ! Register(child.ref)
      expectMsg(RegisterAck)

      val workLoadString = "Test load"

      parent ! Work(workLoadString)

      // the interaction between the parent and the child actor
      child.expectMsg(ChildWork(workLoadString, testActor))
      child.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val parent  = system.actorOf(Props[Parent])
      val child = TestProbe("child")
      parent ! Register(child.ref)
      expectMsg(RegisterAck)

      val workLoadString = "Test load"

      parent ! Work(workLoadString)
      parent ! Work(workLoadString)

      child.receiveWhile() {
        case ChildWork(`workLoadString`, `testActor`)
            => child.reply(WorkCompleted(3, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(6))
    }

  }
}

object TestProbSpec {
  case class Work(text: String)
  case class ChildWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Register(childRef: ActorRef)
  case object RegisterAck
  case class Report(totalCount: Int)

  class Parent extends Actor {
    override def receive: Receive = {
      case Register(childRef) => {
        sender() ! RegisterAck
        context.become(online(childRef, 0))
      }
      case _ => // ignore
    }
    def online(childRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => childRef ! ChildWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(childRef, newTotalWordCount))
    }
  }
}
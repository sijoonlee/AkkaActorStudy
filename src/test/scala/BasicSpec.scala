import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.postfixOps
import scala.util.Random

// suffix should be Spec
class BasicSpec()
  extends TestKit(ActorSystem("BasicSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._

  "A Simple Actor" should {
    "send back the same message" in {
      val simpleActor = system.actorOf(Props[SimpleActor])
      val message = "Hello, Test"
      simpleActor ! message

      expectMsg(message)
    }
  }

  "A BlackHole Actor" should {
    "send back no message" in {
      val blackHoleActor = system.actorOf(Props[BlackHole])
      val message = "Hello"
      blackHoleActor ! message

      // expectMsg(message)
      // assertion failed: timeout (3 seconds) during expectMsg while waiting for Hello
      // akka.test.single-expect-default --> you can change the time-out

      expectNoMessage(1 second)
    }
  }

  "A LabTest Actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])
    "turn a string to uppercase" in {
      labTestActor ! "hey"
      // expectMsg("HEY")
      val reply = expectMsgType[String]
      assert(reply == "HEY")
    }
    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }
    "reply to fruits" in {
      labTestActor ! "fruits"
      expectMsgAllOf("orange", "apple")
    }
    "reply to fruits - another way" in {
      labTestActor ! "fruits"
      val messages = receiveN(2)
      // do more assertion
    }
    "reply to fruits - another way 2" in {
      labTestActor ! "fruits"
      expectMsgPF() { // only care that the Partial Function is defined
        case "apple" =>
        case "orange" =>
      }
    }
  }
}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }
  class BlackHole extends Actor {
    override def receive: Receive = {
      case _ =>
    }
  }
  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "fruits" =>
        sender() ! "orange"
        sender() ! "apple"
      case message: String => sender() ! message.toUpperCase()
    }
  }
}

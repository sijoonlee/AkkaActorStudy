import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, EventFilter, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike  with BeforeAndAfterAll {
  implicit val system = ActorSystem("SynchronousTesting")

  override def afterAll(): Unit = {
    system.terminate()
  }
  import SynchronousTestingSpec._

  "A counter" should {
    // 1. use TestActorRef
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter]) // need an implicit ActorSystem
      counter ! Inc
      assert(counter.underlyingActor.count == 1)
    }
    // technically same test with above
    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc)
      assert(counter.underlyingActor.count == 1)
    }

    // 2. use CallingThreadDispatcher
    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read) // it happens synchronously
      probe.expectMsg(0)

    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read
  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
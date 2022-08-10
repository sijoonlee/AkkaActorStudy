import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogSpec extends TestKit(ActorSystem("InterceptingLogSpec", ConfigFactory.load().getConfig("interceptingLogConfig")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit ={
    TestKit.shutdownActorSystem(system)

    import InterceptingLogSpec._

    var item = "some item"
    var creditCard = "123"
    "A checkout flow" should {
      "correctly log the dispatch" in {
        EventFilter.info(pattern = s"order dispatched: item ${item}, orderId [0-9]+", occurrences = 1) intercept  {
          val checkoutRef = system.actorOf(Props[CheckoutActor])
          checkoutRef ! Checkout(item, creditCard)
        }
      }

      "throw error if payment is denied" in {
       EventFilter[RuntimeException](occurrences = 1) intercept {
         val checkoutRef = system.actorOf(Props[CheckoutActor])
         checkoutRef ! Checkout(item, "0")
       }
      }
    }
  }
}

object InterceptingLogSpec {
  case class Checkout(item: String, creditCard: String)
  case class AuthorizeCard(str: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = ???

    def awaitingCheckout: Receive = {
      case Checkout(item, creditCard) =>
        paymentManager ! AuthorizeCard(creditCard)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied =>
        throw new RuntimeException("I can not handle this")
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed =>
        context.become(awaitingCheckout)
    }
  }
  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(creditCard: String) =>
        if (creditCard.startsWith("0")) sender() ! PaymentAccepted
        else sender() ! PaymentDenied
    }
  }
  class FulfillmentManager extends Actor with ActorLogging {
    var orderId = 0
    override def receive: Receive = {
      case DispatchOrder(item: String) =>
        orderId += 1
        log.info(s"order dispatched: item ${item}, orderId ${orderId}")
        sender() ! OrderConfirmed
    }
  }
}

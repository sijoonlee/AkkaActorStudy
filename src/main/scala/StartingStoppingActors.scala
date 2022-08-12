import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorDemo");


  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }
  class Parent extends Actor with ActorLogging{
    override def receive: Receive = withChildren(Map())

    import Parent._
    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name: String) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child],  name))))
      case StopChild(name: String) =>
        log.info(s"Stopping child $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
        log.info("Stopping myself") // this will stop all children as well
        context.stop(self)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  import Parent._
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("Child1")
  val child2 = system.actorSelection("/user/parent/Child2")
  parent ! StartChild("Child2")

  val child = system.actorSelection("/user/parent/Child1")
  child ! "hi kid"

  parent ! StopChild("Child1") // asynchronously stops
  for( i <- 1 to 50) child ! s"Are you there? $i" // some of these will be delivered to child

  parent ! Stop
  for( i <- 1 to 50) child2 ! s"Hey? $i" // some of these will be delivered to child 2


  // Death Watch
  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the ref <$ref> I am watching has been stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)

  watchedChild ! PoisonPill
}

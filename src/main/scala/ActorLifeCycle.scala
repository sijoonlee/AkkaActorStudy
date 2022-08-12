import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {


  object StartChild
  class LifeCycleActor extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("PreStart")
    override def postStop(): Unit = log.info("PostStop")

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeCycleActor], "child")
    }
  }

  val system = ActorSystem("LifeCycleDemo")
  val parent = system.actorOf(Props[LifeCycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill
  // [akka://LifeCycleDemo/user/parent] PreStart
  // [akka://LifeCycleDemo/user/parent/child] PreStart
  // [akka://LifeCycleDemo/user/parent/child] PostStop
  // [akka://LifeCycleDemo/user/parent] PostStop

  object Fail
  object FailChild
  class Parent extends Actor {
    val child = context.actorOf(Props[Child], "myChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
    }
  }
  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("PreStart")
    override def postStop(): Unit = log.info("PostStop")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"PreRestart ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      log.info(s"PostRestart ${reason.getMessage}")
    }
    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail")
        throw new RuntimeException("Failed")
    }
  }
  val myParent = system.actorOf(Props[Parent])
  myParent ! FailChild
  // [akka://LifeCycleDemo/user/parent] PreStart
  // [akka://LifeCycleDemo/user/parent/child] PreStart
  // [akka://LifeCycleDemo/user/$a/myChild] PreStart
  // [akka://LifeCycleDemo/user/parent/child] PostStop
  // [akka://LifeCycleDemo/user/parent] PostStop
  // [akka://LifeCycleDemo/user/$a/myChild] child will fail
  // [akka://LifeCycleDemo/user/$a/myChild] Failed
  // java.lang.RuntimeException: Failed
  // ...
  // [akka://LifeCycleDemo/user/$a/myChild] PreRestart Failed
  // [akka://LifeCycleDemo/user/$a/myChild] PostRestart Failed
}

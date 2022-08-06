import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {

  // distributed word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) => {
        println("[master] init")
        val childrenRefs = for ( i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
      }
    }
    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] received $text - will send it to $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I received a reply for task $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I received task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val texts = List("I want to go somewhere nice", "Scala is good", "I have no idea", "Really? Yes")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I got reply $count")
    }
  }

  val system = ActorSystem("RoundRobin")
  val testActor = system.actorOf(Props[TestActor], "testActor")

  testActor ! "go"
}

//[master] init
//  [master] received I want to go somewhere nice - will send it to 0
//  [master] received Scala is good - will send it to 1
//  [master] received I have no idea - will send it to 2
//  [master] received Really? Yes - will send it to 0
//  akka://RoundRobin/user/testActor/master/wcw_2 I received task 1 with Scala is good
//  akka://RoundRobin/user/testActor/master/wcw_3 I received task 2 with I have no idea
//  akka://RoundRobin/user/testActor/master/wcw_1 I received task 0 with I want to go somewhere nice
//  akka://RoundRobin/user/testActor/master/wcw_1 I received task 3 with Really? Yes
//  [master] I received a reply for task 1 with 3
//  [test actor] I got reply 3
//  [master] I received a reply for task 2 with 4
//  [master] I received a reply for task 0 with 6
//  [master] I received a reply for task 3 with 2
//  [test actor] I got reply 4
//  [test actor] I got reply 6
//  [test actor] I got reply 2

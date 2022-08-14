import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import scala.io.Source
import java.io.File
import scala.concurrent.duration._
import scala.language.postfixOps

object BackOffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit = {
      log.info("Persistent Actor Starting")
    }
    override def postStop(): Unit = {
      log.warning("Persistent Actor Stop")
    }
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("Persistent Actor Restarting")
    }
    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/importan.txt"))
        log.info("Just read important.txt" + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackOffSupervisorDemo")
  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

  /**
   * simpleSupervisor
   * - child called simpleBackoffActor (props of type FileBasedPersistentActor)
   * - supervison strategy is default, which is restarting on everything
   *   - first attempt after 3 sec
   *   - second attempt will be 2x the previous attempt + random factor
   */
  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  simpleBackoffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistentActor],
      "StopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

  val stopSupervisor = system.actorOf(stopSupervisorProps)
  stopSupervisor ! ReadFile // StopBackoffActor will stop and restart
  // StopBackoffActor] Persistent Actor Starting
  // StopBackoffActor] src/main/resources/testfiles/importan.txt (No such file or directory)
  // StopBackoffActor] Persistent Actor Stop
  // StopBackoffActor] Persistent Actor Starting

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/importan.txt"))
    }
  }

  val eagerActor = system.actorOf(Props[EagerFBPActor])
  // ActorInitializationException => STOP
  // java.io.FileNotFoundException: src/main/resources/testfiles/importan.txt (No such file or directory)
  // akka.actor.ActorInitializationException: akka://BackOffSupervisorDemo/user/$b: exception during creation

  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

  /**
   * EagerSupervisor
   *  - child eagerActor will die with ActorInitializationException
   *  - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
   *  - Backoff will kick in after 1 second, 2s , 4s, ...
   *
   */

}

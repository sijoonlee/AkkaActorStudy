import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {
  class LoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val configString =
    """
      | akka {
      |   loglevel = "DEBUG"
      | }
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system1 = ActorSystem("ConfigDemo", ConfigFactory.load(config))
  val actor1 = system1.actorOf(Props[LoggingActor])
  actor1 ! "m" // will show debug level message

  val system2 = ActorSystem("ConfigDemo2") // this will read resources/application.conf by default
  val actor2 = system2.actorOf(Props[LoggingActor])
  actor2 ! "m" // will show debug level message

  val myConfig = ConfigFactory.load().getConfig("myConfig") // namespace in application.conf
  val myConfigSystem = ActorSystem("myConfigDemo", myConfig)
  val actor3 = myConfigSystem.actorOf(Props[LoggingActor])
  actor3 ! "m"

  val myConfig2 = ConfigFactory.load("anotherFolder/another.conf")
  val myConfigSystem2 = ActorSystem("myConfigDemo", myConfig2)
  val actor4 = myConfigSystem2.actorOf(Props[LoggingActor])
  actor4 ! "m"

  val myConfig3 = ConfigFactory.load("json/myJson.json")
  println(myConfig3.getString("akka.loglevel"))
  val myConfigSystem3 = ActorSystem("myConfigDemo", myConfig3)
  val actor5 = myConfigSystem3.actorOf(Props[LoggingActor])
  actor5 ! "m"

  val myConfig4 = ConfigFactory.load("myProperties.properties")
  println(myConfig4.getString("akka.loglevel"))
  val myConfigSystem4 = ActorSystem("myConfigDemo", myConfig4)
  val actor6 = myConfigSystem4.actorOf(Props[LoggingActor])
  actor6 ! "m"
}

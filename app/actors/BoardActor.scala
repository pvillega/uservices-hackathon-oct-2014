package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.event.LoggingReceive
import play.libs.Akka

class BoardActor extends Actor with ActorLogging {
  var users = Set[ActorRef]()

  def receive = LoggingReceive {
    case m: Message => users map {
      _ ! m
    }
    case Subscribe => {
      users += sender
      context watch sender
    }
    case Terminated(user) => users -= user
  }
}

object BoardActor {
  lazy val board = Akka.system().actorOf(Props[BoardActor])

  def apply() = board
}

case class Message(uuid: String, s: String)

object Subscribe

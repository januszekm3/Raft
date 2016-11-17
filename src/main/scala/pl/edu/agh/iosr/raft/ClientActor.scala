package pl.edu.agh.iosr.raft

import scala.util.{Failure, Random, Success, Try}
import akka.actor.{Actor, ActorLogging, ActorPath, Props}
import pl.edu.agh.iosr.raft.ClientActor.{ClientAck, SetStateToRandomNode}
import pl.edu.agh.iosr.raft.structure.Messages.SetNumberToLeader
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * @author lewap
  * @since 15.11.16
  */
class ClientActor(paths: List[ActorPath]) extends Actor with ActorLogging {
  implicit val timeout: Timeout = 3 seconds

  override def receive: Receive = {
    case SetStateToRandomNode(number) =>
      sendToRandomNodeUpToSuccess(number)
  }

  private def sendToRandomNodeUpToSuccess(number: Int): Unit = {
    val randomPath = paths(Random.nextInt(paths.size))
    val randomNode = context.actorSelection(randomPath)

    log.info(s"client tries to send $number to ${randomPath.name}")
    val futureResult = randomNode ? SetNumberToLeader(number)
    Try(Await.result(futureResult, 3 seconds)) match {
      case Success(msg) =>
        msg match {
          case msg@ClientAck(nr) => log.info(s"Client received $msg from ${sender().path.name}")
          case other => log.warning(s"Received unexpected msg $other")
        }
      case Failure(throwable) =>
        log.info(s"client failed to send $number to ${randomPath.name}. Retrying...")
        sendToRandomNodeUpToSuccess(number)
    }
  }
}

object ClientActor {

  def props(paths: List[ActorPath]): Props =
    Props(new ClientActor(paths))

  case class SetStateToRandomNode(number: Int)

  case class ClientAck(number: Int)

}
package pl.edu.agh.iosr.raft.api

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import pl.edu.agh.iosr.raft.ClientActor
import pl.edu.agh.iosr.raft.ClientActor.SetStateToRandomNode
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode
import pl.edu.agh.iosr.raft.structure.ServerNode.InternalState

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * @author lewap
  * @since 17.11.16
  */
class RaftManager extends Actor with ActorLogging {

  import RaftManager._

  implicit val timeout: Timeout = 2 seconds

  var nodes: List[ActorRef] = List()
  var paths: List[ActorPath] = List()
  var client: Option[ActorRef] = None

  override def receive: Receive = {

    case Initialize(nodesQuantity) =>
      if (nodes.isEmpty) {
        log.info(s"Initializing $nodesQuantity nodes")
        nodes = initializeNodes(nodesQuantity)
        paths = nodes.map(_.path)
        client = Some(context.actorOf(ClientActor.props(paths)))
      }

    case KillNode(number) =>
      if (number >= 1 && number <= nodes.size) {
        log.info(s"Killing ${nodeNameFrom(number)}")
        val nodeToKill = nodes(number - 1)
        nodeToKill ! PoisonPill
      }

    case StartNode(number) =>
      if (number >= 1 && number <= nodes.size) {
        val nodeName = nodeNameFrom(number)
        log.info(s"Trying to create $nodeName")
        val reborn = context.actorOf(ServerNode.props(), nodeName)
        val path = paths(number - 1)
        reborn ! AddNodes(paths.toSet - path)
      }

    case msg@SetStateToRandomNode(number) =>
      client.foreach(_ ! msg)

    case GetState =>
      val states = paths.reverse
        .map(context.actorSelection)
        .foldLeft(List[InternalState]()) { case (list, node) =>
          val futureResult = (node ? GetCurrentState).mapTo[InternalState]
          Try(Await.result(futureResult, 2 seconds)) match {
            case Success(state) => state :: list
            case Failure(throwable) => list
          }
        }
      sender() ! states

  }

  private def initializeNodes(quantity: Int): List[ActorRef] = {
    val iterator = (1 to quantity).iterator
    val nodesList = List.fill(quantity) {
      context.actorOf(ServerNode.props(), nodeNameFrom(iterator.next()))
    }

    val nodesSet = nodesList.toSet
    nodesSet foreach { node =>
      val otherNodes = nodesSet - node
      node ! AddNodes(otherNodes.map(_.path))
    }

    nodesList
  }

}

object RaftManager {

  def props(): Props =
    Props(new RaftManager())

  def nodeNameFrom(number: Int): String =
    "node" + number

  case class Initialize(nodesQuantity: Int)

  case class KillNode(number: Int)

  case class StartNode(number: Int)

  case object GetState

}
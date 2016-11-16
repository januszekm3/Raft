package pl.edu.agh.iosr.raft

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import pl.edu.agh.iosr.raft.ClientActor.SetStateToRandomNode
import pl.edu.agh.iosr.raft.structure.Messages.{AddNodes, PrintCurrentState}
import pl.edu.agh.iosr.raft.structure.ServerNode

object RunnerExample {

  implicit val system = ActorSystem("RaftActorSystem")
  val nodes = initializeNodes(Settings.nodesQuantity)
  val paths = nodes.map(_.path)

  val client = system.actorOf(ClientActor.props(paths), "client")

  def main(args: Array[String]): Unit = {
    printSystemState()

    val nodeToKill = nodes.head
    val name = nodeToKill.path.name
    nodeToKill ! PoisonPill

    TimeUnit.SECONDS.sleep(2)
    client ! SetStateToRandomNode
    TimeUnit.SECONDS.sleep(15)

    val newNode = system.actorOf(ServerNode.props(), name)
    printSystemState()

  }

  private def initializeNodes(quantity: Int): List[ActorRef] = {
    val iterator = (1 to quantity).iterator
    val nodesList = List.fill(quantity) {
      system.actorOf(ServerNode.props(), s"node${iterator.next()}")
    }

    val nodesSet = nodesList.toSet
    nodesSet foreach { node =>
      val otherNodes = nodesSet - node
      node ! AddNodes(otherNodes.map(_.path))
    }

    nodesList
  }

  private def printSystemState(): Unit = {
    paths foreach { path =>
      system.actorSelection(path) ! PrintCurrentState
    }
  }

}

package pl.edu.agh.iosr.raft

import akka.actor.{ActorRef, ActorSystem}
import pl.edu.agh.iosr.raft.structure.Messages.{AddNodes, PrintCurrentState}
import pl.edu.agh.iosr.raft.structure.ServerNode

object Runner {

  implicit val system = ActorSystem("RaftActorSystem")

  def main(args: Array[String]): Unit = {
    val nodes = initializeNodes(Settings.nodesQuantity)

    nodes foreach { node =>
      node ! PrintCurrentState
    }
  }

  private def initializeNodes(quantity: Int): List[ActorRef] = {
    val iterator = (1 to quantity).iterator
    val nodesList = List.fill(quantity) {
      system.actorOf(ServerNode.props(), s"node${iterator.next()}")
    }

    val nodesSet = nodesList.toSet
    nodesSet foreach { node =>
      val otherNodes = nodesSet - node
      node ! AddNodes(otherNodes)
    }

    nodesList
  }

}

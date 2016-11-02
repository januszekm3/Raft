package pl.edu.agh.iosr.raft

import akka.actor.{ActorRef, ActorSystem}
import pl.edu.agh.iosr.raft.structure.Messages.{AddNodes, PrintCurrentState}
import pl.edu.agh.iosr.raft.structure.ServerNode

/**
  * @author lewap
  * @since 01.11.16
  */
object Runner {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("RaftActorSystem")

    val nodesQuantity = 2
    val iterator = (1 to nodesQuantity).iterator
    val nodes: List[ActorRef] = List.fill(nodesQuantity)(
      system.actorOf(ServerNode.props(), s"node${iterator.next()}")
    )

    nodes foreach { node =>
      val otherNodes = (nodes.toSet - node).toList
      node ! AddNodes(otherNodes)
    }

    nodes foreach { node =>
      node ! PrintCurrentState
    }

  }

}

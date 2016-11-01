package pl.edu.agh.iosr.raft

import akka.actor.ActorSystem
import pl.edu.agh.iosr.raft.structure.Messages.ChangeState
import pl.edu.agh.iosr.raft.structure.ServerNode
import pl.edu.agh.iosr.raft.structure.State.Leader

/**
  * @author lewap
  * @since 01.11.16
  */
object Runner {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("RaftActorSystem")

    val node = system.actorOf(ServerNode.props(), "node01")
    node ! ChangeState(Leader)

  }

}

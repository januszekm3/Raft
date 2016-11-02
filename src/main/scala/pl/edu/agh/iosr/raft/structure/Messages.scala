package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorRef
import pl.edu.agh.iosr.raft.structure.State.State

/**
  * @author lewap
  * @since 02.11.16
  */
object Messages {

  case class ChangeState(newState: State)

  case class AddNodes(nodes: List[ActorRef])

  case object HeartBeat

  case object PrintCurrentState

  case object Timeout

}

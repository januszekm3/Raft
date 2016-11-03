package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorRef
import pl.edu.agh.iosr.raft.structure.State.State

object Messages {

  case class ChangeState(newState: State)

  case class AddNodes(nodes: Set[ActorRef])

  case class SetNumber(number: Int)

  case class AddNumber(number: Int)

  case object LeaderRequest

  case object LeaderRequestAccepted

  case object NewLeader

  case object Heartbeat

  case object PrintCurrentState

  case object ServerTimeout

}

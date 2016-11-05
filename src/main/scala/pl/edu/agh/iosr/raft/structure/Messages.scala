package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorRef
import pl.edu.agh.iosr.raft.structure.State.State

object Messages extends ActionMessages with GetterMessages

trait ActionMessages {

  case class AddNodes(nodes: Set[ActorRef])

  case class SetNumberToLeader(number: Int)

  case class SetNumberRequest(number: Int, uuid: String)

  case class SetNumberAck(number: Int, uuid: String)

  case class SetNumberCommit(number: Int)

  case class AddNumberToLeader(number: Int)

  case class AddNumber(number: Int, uuid: String)

  case object LeaderRequest

  case object LeaderRequestAccepted

  case object NewLeader

  case object Heartbeat

  case object ServerTimeout

}

trait GetterMessages {

  case object GetCurrentState

  case object PrintCurrentState

}


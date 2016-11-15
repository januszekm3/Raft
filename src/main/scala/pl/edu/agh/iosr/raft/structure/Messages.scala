package pl.edu.agh.iosr.raft.structure

import java.util.Date

import akka.actor.ActorPath

object Messages extends ActionMessages with GetterMessages

trait ActionMessages {

  case class AddNodes(nodes: Set[ActorPath])

  case class SetNumberToLeader(number: Int)

  case class SetNumberRequest(number: Int, uuid: String)

  case class SetNumberAck(number: Int, uuid: String)

  case class SetNumberCommit(number: Int, commitDate: Option[Date])

  case class AddNumberToLeader(number: Int)

  case class AddNumber(number: Int, uuid: String)

  case class StateUpdate(number: Int, commitDate: Option[Date])

  case object LeaderRequest

  case object LeaderRequestAccepted

  case object NewLeader

  case class Heartbeat(stateDate: Option[Date])

  case object ServerTimeout

  case object StateUpdateRequest

}

trait GetterMessages {

  case object GetCurrentState

  case object PrintCurrentState

}


package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorRef
import pl.edu.agh.iosr.raft.structure.State.State

object Messages extends ActionMessages with GetterMessages

trait ActionMessages {

  case class AddNodes(nodes: Set[ActorRef])

  case class SetNumber(number: Int)

  case class AddNumber(number: Int)

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


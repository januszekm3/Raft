package pl.edu.agh.iosr.raft.structure

import akka.actor.{Actor, ActorLogging, Props}
import pl.edu.agh.iosr.raft.structure.Messages.ChangeState
import pl.edu.agh.iosr.raft.structure.State._

/**
  * @author lewap
  * @since 02.11.16
  */
class ServerNode extends Actor with ActorLogging {

  var state: State = Follower
  var logs: List[String] = List()

  override def receive: Receive = {
    case ChangeState(newState) =>
      log.debug(s"Changing state from $state to $newState")
      state = newState

    case any =>
      log.warning(s"Received unexpected message $any")
  }

}

object ServerNode {
  def props(): Props =
    Props(new ServerNode())
}

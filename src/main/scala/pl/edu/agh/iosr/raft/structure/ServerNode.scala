package pl.edu.agh.iosr.raft.structure

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.edu.agh.iosr.raft.structure.Messages.{AddNodes, ChangeState, HeartBeat, PrintCurrentState}
import pl.edu.agh.iosr.raft.structure.ServerNode.InternalHeartbeat
import pl.edu.agh.iosr.raft.structure.State._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/**
  * @author lewap
  * @since 02.11.16
  */
class ServerNode extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  var state: State = Follower
  var otherNodes: List[ActorRef] = List()

  var scheduler = createScheduler()

  override def receive: Receive = {

    case InternalHeartbeat =>
      log.debug("Received internal heartbeat")
      otherNodes foreach { node =>
        node ! HeartBeat
      }

    case HeartBeat =>
      log.debug("Received heartbeat")
      scheduler.cancel()
      scheduler = createScheduler()

    case AddNodes(nodesToAppend) =>
      otherNodes = nodesToAppend ::: otherNodes

    case ChangeState(newState) =>
      log.debug(s"Changing state from $state to $newState")
      state = newState

    case PrintCurrentState =>
      println(s"name = ${self.path.name}")
      println(s"  state = $state")
      println(s"  other nodes = $otherNodes")

    case any =>
      log.warning(s"Received unexpected message $any")
  }

  private def createScheduler() = {
    context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = (5 + Random.nextInt(3)) seconds,
      self,
      InternalHeartbeat
    )
  }

}

object ServerNode {
  def props(): Props =
    Props(new ServerNode())

  case object InternalHeartbeat

}

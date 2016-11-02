package pl.edu.agh.iosr.raft.structure

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode.InternalHeartbeat
import pl.edu.agh.iosr.raft.structure.State._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class ServerNode extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  var state: State = Follower
  var otherNodes: Set[ActorRef] = Set()
  var heartbeatScheduler = createHeartbeatScheduler()
  var timeOutScheduler = createTimeOutScheduler()

  override def receive: Receive = {

    case InternalHeartbeat =>
      log.debug("Received internal heartbeat")
      otherNodes foreach { node =>
        node ! HeartBeat
      }

    case HeartBeat =>
      log.debug(s"Received heartbeat from ${sender().path.name}")
      timeOutScheduler.cancel()
      timeOutScheduler = createTimeOutScheduler()

    case ServerTimeout =>
      log.debug("Received timeout")
      heartbeatScheduler.cancel()
      timeOutScheduler.cancel()

    case AddNodes(nodesToAppend) =>
      otherNodes ++= nodesToAppend

    case ChangeState(newState) =>
      log.debug(s"Changing state from $state to $newState")
      state = newState

    case PrintCurrentState =>
      println(
        s"""name = ${self.path.name}
            |  state = $state
            |  other nodes = ${otherNodes.map(_.path.name)}
         """.stripMargin
      )

    case any =>
      log.warning(s"Received unexpected message $any")

  }

  private def createHeartbeatScheduler() = {
    context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = (5 + Random.nextInt(3)) seconds,
      self,
      InternalHeartbeat
    )
  }

  private def createTimeOutScheduler() = {
    val timeout = 2 + Random.nextInt(5)
    context.system.scheduler.scheduleOnce(timeout seconds, self, ServerTimeout)
  }

}

object ServerNode {
  def props(): Props =
    Props(new ServerNode())

  case object InternalHeartbeat

}

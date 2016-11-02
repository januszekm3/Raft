package pl.edu.agh.iosr.raft.structure

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode.InternalHeartbeat
import pl.edu.agh.iosr.raft.structure.State._

import scala.language.postfixOps

class ServerNode(schedulersConfig: SchedulersConfig) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher
  val systemScheduler = context.system.scheduler

  var state: State = Follower
  var otherNodes: Set[ActorRef] = Set()
  var heartbeatScheduler = createHeartbeatScheduler()
  var timeoutScheduler = createTimeoutScheduler()

  override def receive: Receive = {

    case InternalHeartbeat =>
      log.debug("Received internal heartbeat")
      otherNodes foreach { node =>
        node ! Heartbeat
      }

    case Heartbeat =>
      log.debug(s"Received heartbeat from ${sender().path.name}")
      timeoutScheduler.cancel()
      timeoutScheduler = createTimeoutScheduler()

    case ServerTimeout =>
      log.debug("Received server timeout")
      heartbeatScheduler.cancel()
      timeoutScheduler.cancel()

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
    systemScheduler.schedule(
      initialDelay = schedulersConfig.initialHeartbeatDelay,
      interval = schedulersConfig.heartbeatInterval,
      self,
      InternalHeartbeat
    )
  }

  private def createTimeoutScheduler() = {
    systemScheduler.scheduleOnce(schedulersConfig.timeout, self, ServerTimeout)
  }

}

object ServerNode {

  def props(schedulersConfig: SchedulersConfig = SchedulersConfig.random()): Props =
    Props(new ServerNode(schedulersConfig))

  case object InternalHeartbeat

}

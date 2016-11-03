package pl.edu.agh.iosr.raft.structure

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode.InternalHeartbeat
import pl.edu.agh.iosr.raft.structure.State._

import scala.language.postfixOps

class ServerNode(schedulersConfig: SchedulersConfig) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher
  val systemScheduler = context.system.scheduler

  var state: State = Follower
  var otherNodes: Set[ActorRef] = Set()
  var leader: Option[ActorRef] = None
  var heartbeatScheduler: Option[Cancellable] = None
  var timeoutScheduler = createTimeoutScheduler()

  var number: Int = 0
  var leaderRequestAcceptedCounter = 0

  override def receive: Receive = {

    case InternalHeartbeat =>
      log.debug("Received internal heartbeat")
      sendToOthers(Heartbeat)

    case Heartbeat =>
      log.debug(s"Received heartbeat from ${sender().path.name}")
      timeoutScheduler.cancel()
      timeoutScheduler = createTimeoutScheduler()

    case SetNumber(newNumber) =>
    // TODO

    case AddNumber(numberToAdd) =>
    // TODO

    case msg@LeaderRequest =>
      log.debug(s"$msg received from ${sender().path.name}")
      if (state == Follower) {
        sender() ! LeaderRequestAccepted
      }

    case msg@LeaderRequestAccepted =>
      log.debug(s"$msg received from ${sender().path.name}")
      leaderRequestAcceptedCounter += 1
      val previousState = state
      if (2 * (leaderRequestAcceptedCounter + 1) > otherNodes.size + 1) {
        state = Leader
        leader = Some(self)
        heartbeatScheduler = Some(createHeartbeatScheduler())
        if (previousState != state) {
          sendToOthers(NewLeader)
        }
      }

    case msg@NewLeader =>
      log.debug(s"$msg received from ${sender().path.name}")
      leader = Some(sender())
      state = Follower

    case ServerTimeout =>
      log.debug("Received server timeout")
      heartbeatScheduler.foreach(_.cancel())
      timeoutScheduler.cancel()
      state = Candidate
      sendToOthers(LeaderRequest)

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

  private def sendToOthers(msg: Any): Unit = {
    otherNodes foreach { node =>
      node ! msg
    }
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

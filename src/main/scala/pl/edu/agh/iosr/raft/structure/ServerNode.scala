package pl.edu.agh.iosr.raft.structure

import java.util.{Date, UUID}

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, Cancellable, Props}
import akka.event.LoggingReceive
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode.{InternalHeartbeat, InternalState}
import pl.edu.agh.iosr.raft.structure.State._

import scala.language.postfixOps

class ServerNode(schedulersConfig: SchedulersConfig) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher
  val systemScheduler = context.system.scheduler

  var state: State = Follower
  var otherNodes: Set[ActorPath] = Set()
  var leader: Option[ActorRef] = None
  var heartbeatScheduler: Option[Cancellable] = None
  var timeoutScheduler = createTimeoutScheduler()

  var number: Int = 0
  var setNumberAcks: Map[String, Int] = Map()
  var leaderRequestAcceptedCounter = 0
  var lastSuccessfulCommitDate: Option[Date] = None

  override def receive: Receive = actionsReceive orElse otherReceive

  def actionsReceive: Receive = {
    case InternalHeartbeat =>
      log.debug("Received internal heartbeat")
      sendToOthers(Heartbeat(lastSuccessfulCommitDate))

    case Heartbeat(leaderLastCommitDate) =>
      if (!lastSuccessfulCommitDate.equals(leaderLastCommitDate)) {
        sender() ! StateUpdateRequest
      }
      log.debug(s"Received heartbeat from ${sender().path.name}")
      timeoutScheduler.cancel()
      timeoutScheduler = createTimeoutScheduler()

    case msg@StateUpdateRequest =>
      log.debug(s"$msg received from ${sender().path.name}")
      sender() ! StateUpdate(number, lastSuccessfulCommitDate)

    case msg@StateUpdate(newNumber, commitDate) =>
      log.debug(s"$msg received from ${sender().path.name}")
      number = newNumber
      lastSuccessfulCommitDate = commitDate

    case action@SetNumberToLeader(newNumber) =>
      log.debug(s"$action received from ${sender().path.name}")
      if (state != Leader) {
        leader.foreach(_ forward action)
      } else {
        val uuid = UUID.randomUUID().toString
        setNumberAcks += uuid -> 1
        sendToOthers(SetNumberRequest(newNumber, uuid))
      }

    case msg@SetNumberRequest(newNumber, uuid) =>
      log.debug(s"$msg received from ${sender().path.name}")
      sender() ! SetNumberAck(newNumber, uuid)

    case msg@SetNumberAck(newNumber, uuid) =>
      log.debug(s"$msg received from ${sender().path.name}")
      setNumberAcks += uuid -> (setNumberAcks(uuid) + 1)
      if (2 * setNumberAcks(uuid) > otherNodes.size + 1) {
        number = newNumber
        if (2 * (setNumberAcks(uuid) - 1) <= otherNodes.size + 1) {
          lastSuccessfulCommitDate = Some(new Date())

          sendToOthers(SetNumberCommit(newNumber, lastSuccessfulCommitDate))
        }
      }

    case msg@SetNumberCommit(newNumber, commitDate) =>
      log.debug(s"$msg received from ${sender().path.name}")
      number = newNumber
      lastSuccessfulCommitDate = commitDate
      // TODO tu odsyłamy klientowi, że ok

    case AddNumberToLeader(numberToAdd) =>
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
        leaderRequestAcceptedCounter = 0
        if (previousState != state) {
          sendToOthers(NewLeader)
        }
      }

    case msg@NewLeader =>
      log.debug(s"$msg received from ${sender().path.name}")
      leader = Some(sender())
      state = Follower
      heartbeatScheduler.foreach(_.cancel())
      leaderRequestAcceptedCounter = 0

    case ServerTimeout =>
      log.debug("Received server timeout")
      heartbeatScheduler.foreach(_.cancel())
      state = Candidate
      sendToOthers(LeaderRequest)

    case AddNodes(nodesToAppend) =>
      otherNodes ++= nodesToAppend
  }

  def otherReceive: Receive = {
    case GetCurrentState =>
      sender() ! currentState()

    case PrintCurrentState =>
      println(
        s"""name = ${self.path.name}
            |  state = $state
            |  other nodes = ${otherNodes.map(_.name)}
         """.stripMargin
      )

    case any =>
      log.warning(s"Received unexpected message $any")
  }

  private def sendToOthers(msg: Any): Unit = {
    val actors = otherNodes map { nodePath =>
      context.actorSelection(nodePath)
    }

    actors foreach { node =>
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

  private def currentState(): InternalState =
    InternalState(state, otherNodes, leader, heartbeatScheduler, timeoutScheduler, number, leaderRequestAcceptedCounter)

}

object ServerNode {

  def props(schedulersConfig: SchedulersConfig = SchedulersConfig.random()): Props =
    Props(new ServerNode(schedulersConfig))

  case object InternalHeartbeat

  case class InternalState(state: State, otherNodes: Set[ActorPath], leader: Option[ActorRef],
                           heartbeatScheduler: Option[Cancellable], timeoutScheduler: Cancellable,
                           number: Int, leaderRequestAcceptedCounter: Int)

}

package pl.edu.agh.iosr.raft.structure

import java.util.{Date, UUID}

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, Cancellable, Props}
import pl.edu.agh.iosr.raft.ClientActor.ClientAck
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
      log.info("Received internal heartbeat")
      sendToOthers(Heartbeat(lastSuccessfulCommitDate))

    case Heartbeat(leaderLastCommitDate) =>
      if (!lastSuccessfulCommitDate.equals(leaderLastCommitDate)) {
        sender() ! StateUpdateRequest
      }
      log.info(s"Received heartbeat from ${sender().path.name}")
      timeoutScheduler.cancel()
      timeoutScheduler = createTimeoutScheduler()

    case msg@StateUpdateRequest =>
      log.info(s"$msg received from ${sender().path.name}")
      sender() ! StateUpdate(number, lastSuccessfulCommitDate)

    case msg@StateUpdate(newNumber, commitDate) =>
      log.info(s"$msg received from ${sender().path.name}")
      number = newNumber
      lastSuccessfulCommitDate = commitDate
      printCurrentState()

    case action@SetNumberToLeader(newNumber) =>
      log.info(s"$action received from ${sender().path.name}")
      if (state != Leader) {
        leader.foreach(_ forward action)
      } else {
        val uuid = UUID.randomUUID().toString
        setNumberAcks += uuid -> 1
        sendToOthers(SetNumberRequest(newNumber, uuid, client = sender()))
      }

    case msg@SetNumberRequest(newNumber, uuid, client) =>
      log.info(s"$msg received from ${sender().path.name}")
      val leader = sender()
      leader ! SetNumberAck(newNumber, uuid, client)

    case msg@SetNumberAck(newNumber, uuid, client) =>
      log.info(s"$msg received from ${sender().path.name}")
      setNumberAcks += uuid -> (setNumberAcks(uuid) + 1)
      if (2 * setNumberAcks(uuid) > otherNodes.size + 1) {
        number = newNumber
        if (2 * (setNumberAcks(uuid) - 1) <= otherNodes.size + 1) {
          lastSuccessfulCommitDate = Some(new Date())

          client ! ClientAck(number)

          sendToOthers(SetNumberCommit(newNumber, lastSuccessfulCommitDate))
        }
      }

    case msg@SetNumberCommit(newNumber, commitDate) =>
      log.info(s"$msg received from ${sender().path.name}")
      number = newNumber
      lastSuccessfulCommitDate = commitDate
      printCurrentState()

    case msg@LeaderRequest =>
      log.info(s"$msg received from ${sender().path.name}")
      if (state == Follower) {
        sender() ! LeaderRequestAccepted
      }

    case msg@LeaderRequestAccepted =>
      log.info(s"$msg received from ${sender().path.name}")
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
      log.info(s"$msg received from ${sender().path.name}")
      leader = Some(sender())
      state = Follower
      heartbeatScheduler.foreach(_.cancel())
      leaderRequestAcceptedCounter = 0

    case ServerTimeout =>
      log.info("Received server timeout")
      heartbeatScheduler.foreach(_.cancel())
      state = Candidate
      sendToOthers(LeaderRequest)

    case AddNodes(nodesToAppend) =>
      otherNodes ++= nodesToAppend

    case RemoveNode(node) =>
      otherNodes -= node
  }

  def otherReceive: Receive = {
    case GetCurrentState =>
      sender() ! currentState()

    case PrintCurrentState =>
      printCurrentState()

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
    InternalState(self.path.name, state, otherNodes, leader, heartbeatScheduler, timeoutScheduler, number, leaderRequestAcceptedCounter,
      lastSuccessfulCommitDate)

  private def printCurrentState(): Unit = {
    log.info(
      s"""|
          |name = ${self.path.name}
          |  state = $state
          |  other nodes = ${otherNodes.map(_.name)}
          |  number = $number
         """.stripMargin
    )
  }

}

object ServerNode {

  def props(schedulersConfig: SchedulersConfig = SchedulersConfig.random()): Props =
    Props(new ServerNode(schedulersConfig))

  case object InternalHeartbeat

  case class InternalState(name: String, state: State, otherNodes: Set[ActorPath], leader: Option[ActorRef],
                           heartbeatScheduler: Option[Cancellable], timeoutScheduler: Cancellable,
                           number: Int, leaderRequestAcceptedCounter: Int, lastSuccessfulCommitDate: Option[Date])

}

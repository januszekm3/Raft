package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode.{InternalHeartbeat, InternalState}
import pl.edu.agh.iosr.raft.structure.State.{Candidate, Follower}

import scala.concurrent.duration._
import scala.language.postfixOps

class ServerNodeTest extends TestKit(ActorSystem("ServerNodeTestSystem")) with WordSpecLike with Matchers
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "server node" when {

    "testing external heartbeats" should {
      val probe = TestProbe()
      implicit val sender = probe.ref

      val schedulersConfig = SchedulersConfig(timeout = 1.1 second)
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(Set(sender))

      "expect no msg during timeout time after heartbeat" in {
        serverNode ! Heartbeat
        probe.expectNoMsg(1 seconds)
      }

      "be in follower state after heartbeat" in {
        serverNode ! Heartbeat

        serverNode ! GetCurrentState
        probe.expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Follower
            state.heartbeatScheduler shouldBe None
            state.leaderRequestAcceptedCounter shouldEqual 0
            state.leader shouldBe None
        }
      }

      "send leader request message in case of no heartbeat" in {
        probe.expectMsg(1.2 second, LeaderRequest)
      }

      "become candidate" in {
        serverNode ! GetCurrentState
        probe.expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Candidate
            state.heartbeatScheduler shouldBe None
            state.leaderRequestAcceptedCounter shouldEqual 0
            state.leader shouldBe None
        }
      }
    }

    "sending InternalHeartbeat" should {
      val testProbes = List.fill(3)(TestProbe()).toSet
      val schedulersConfig = SchedulersConfig(timeout = 5 seconds)
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(testProbes.map(_.ref))

      "propagate heartbeat to all other nodes" in {
        serverNode ! InternalHeartbeat

        testProbes foreach { probe =>
          probe.expectMsg(Heartbeat)
        }
      }
    }

    "sending LeaderRequest" should {
      val probe = TestProbe()
      implicit val sender = probe.ref

      val schedulersConfig = SchedulersConfig(timeout = 3 seconds)
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(Set(sender))

      "respond with LeaderRequestAccepted message being in follower state" in {
        serverNode ! GetCurrentState
        probe.expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Follower
        }

        serverNode ! LeaderRequest
        probe.expectMsg(LeaderRequestAccepted)
      }

      "do nothing after timeout and becoming candidate" in {
        probe.expectMsg(LeaderRequest) // timeout occurred

        serverNode ! GetCurrentState
        probe.expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Candidate
        }

        serverNode ! LeaderRequest
        probe.expectNoMsg(3 seconds)
      }
    }

    "sending LeaderRequestAccepted given 2 nodes (including itself)" should {
      val probe = TestProbe()
      implicit val sender = probe.ref

      val schedulersConfig = SchedulersConfig(
        initialHeartbeatDelay = 30 seconds,
        heartbeatInterval = 2 seconds,
        timeout = 1 seconds
      )
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(Set(sender))

      "increase leaderRequestAcceptedCounter" in {
        probe.expectMsg(LeaderRequest)

        serverNode ! GetCurrentState
        probe.expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Candidate
        }

        serverNode ! LeaderRequestAccepted
        probe.expectMsg(NewLeader)
      }
    }

  }

}

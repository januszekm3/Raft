package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import jdk.nashorn.internal.runtime.Debug
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import pl.edu.agh.iosr.raft.structure.Messages._
import pl.edu.agh.iosr.raft.structure.ServerNode.{InternalHeartbeat, InternalState}
import pl.edu.agh.iosr.raft.structure.State.{Candidate, Follower}

import scala.concurrent.duration._
import scala.language.postfixOps

class ServerNodeTest extends TestKit(ActorSystem("ServerNodeTestSystem")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "server node" when {

    "testing external heartbeats" should {
      val schedulersConfig = SchedulersConfig(timeout = 1.1 second)
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(Set(self))

      "expect no msg during timeout time after heartbeat" in {
        expectNoMsg(0.6 second)

        serverNode ! Heartbeat
        expectNoMsg(1 seconds)
      }

      "be in follower state after heartbeat" in {
        serverNode ! Heartbeat

        serverNode ! GetCurrentState
        expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Follower
            state.heartbeatScheduler shouldBe None
            state.leaderRequestAcceptedCounter shouldEqual 0
            state.leader shouldBe None
        }
      }

      "send leader request message in case of no heartbeat" in {
        expectMsg(1.2 second, LeaderRequest)
      }

      "become candidate" in {
        serverNode ! GetCurrentState
        expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Candidate
            state.heartbeatScheduler shouldBe None
            state.leaderRequestAcceptedCounter shouldEqual 0
            state.leader shouldBe None
        }
      }
    }

    "sending InternalHeartbeat" should {
      val schedulersConfig = SchedulersConfig(timeout = 5 seconds)
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      val testProbes = List.fill(3)(TestProbe()).toSet
      serverNode ! AddNodes(testProbes.map(_.ref))

      "propagate heartbeat to all other nodes" in {
        serverNode ! InternalHeartbeat

        testProbes foreach { probe =>
          probe.expectMsg(Heartbeat)
        }
      }
    }

    s"sending LeaderRequest" should {
      val schedulersConfig = SchedulersConfig(timeout = 3 seconds)
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(Set(self))

      "respond with LeaderRequestAccepted message being in follower state" in {
        serverNode ! GetCurrentState
        expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Follower
        }

        serverNode ! LeaderRequest
        expectMsg(LeaderRequestAccepted)
      }

      "do nothing after timeout and becoming candidate" in {
        expectMsg(LeaderRequest) // timeout occurred

        serverNode ! GetCurrentState
        expectMsgPF() {
          case state: InternalState =>
            state.state shouldEqual Candidate
        }

        serverNode ! LeaderRequest
        expectNoMsg(3 seconds)
      }
    }

  }

}

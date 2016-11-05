package pl.edu.agh.iosr.raft.structure

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import pl.edu.agh.iosr.raft.structure.Messages.{AddNodes, LeaderRequest}

import scala.concurrent.duration._
import scala.language.postfixOps

class ServerNodeTest extends TestKit(ActorSystem("ServerNodeTestSystem")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "server node" when {

    "receives no heartbeat" should {
      val schedulersConfig = SchedulersConfig(
        initialHeartbeatDelay = 0 seconds,
        heartbeatInterval = 2 seconds,
        timeout = 1 second
      )
      val serverNode = system.actorOf(ServerNode.props(schedulersConfig))
      serverNode ! AddNodes(Set(self))

      "send leader request message" in {
        expectMsg(1 second, LeaderRequest)
      }
    }
  }

}

package pl.edu.agh.iosr.raft

import akka.actor.{Actor, ActorPath, Props}
import pl.edu.agh.iosr.raft.ClientActor.SetStateToRandomNode

import scala.util.Random

/**
  * @author lewap
  * @since 15.11.16
  */
class ClientActor(paths: List[ActorPath]) extends Actor {
  override def receive: Receive = {
    case SetStateToRandomNode =>
      val randomNode = context.actorSelection(
        paths(Random.nextInt(paths.size))
      )

  }
}

object ClientActor {

  def props(paths: List[ActorPath]): Props =
    Props(new ClientActor(paths))

  case object SetStateToRandomNode

}
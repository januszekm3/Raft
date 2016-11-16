package pl.edu.agh.iosr.raft

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.edu.agh.iosr.raft.api.RaftController

/**
  * @author lewap
  * @since 16.11.16
  */
object Runner{
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("raftActorSystem")
    implicit val materializer = ActorMaterializer()

    val controller = new RaftController()
    Http().bindAndHandle(controller.routes, "localhost", 9000)
  }
}

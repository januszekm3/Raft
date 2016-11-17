package pl.edu.agh.iosr.raft

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.edu.agh.iosr.raft.api.RaftController

/**
  * @author lewap
  * @since 16.11.16
  */
object RunnerHttp{
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("raftActorSystem")
    implicit val materializer = ActorMaterializer()

    val controller = new RaftController()
    Http().bindAndHandle(controller.routes, Settings.host, Settings.port)
    log.info(s"${Settings.port}")
    System.out.println(Settings.port)
  }
}

package pl.edu.agh.iosr.raft

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pl.edu.agh.iosr.raft.api.RaftController
import util.Properties

/**
  * @author lewap
  * @since 16.11.16
  */
object RunnerHttp{
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("raftActorSystem")
    implicit val materializer = ActorMaterializer()

    val controller = new RaftController()
    val myPort = Properties.envOrElse("PORT", "9000").toInt
    Http().bindAndHandle(controller.routes, Settings.host, myPort)
  }
}

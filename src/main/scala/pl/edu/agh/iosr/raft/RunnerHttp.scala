package pl.edu.agh.iosr.raft

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import pl.edu.agh.iosr.raft.api.RaftController

/**
  * @author lewap
  * @since 16.11.16
  */
object RunnerHttp extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("raftActorSystem")
    implicit val materializer = ActorMaterializer()

    val controller = new RaftController()
    Http().bindAndHandle(controller.routes, Settings.host, Settings.port)
    logger.info("Host: " + Settings.host)
    logger.info("Port: " + Settings.port)
  }
}
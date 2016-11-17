package pl.edu.agh.iosr.raft.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import pl.edu.agh.iosr.raft.structure.ServerNode
import pl.edu.agh.iosr.raft.structure.Messages._

/**
  * @author lewap
  * @since 16.11.16
  */
class RaftController extends ErrorHandling {

  implicit val system = ActorSystem("RaftHttp")
  val manager = system.actorOf(RaftManager.props(), "RaftManager")

  def routes: Route =
    handleExceptions(exceptionHandler)(endpoints)

  private def endpoints: Route = {
    pathPrefix("raft") {
      get {
        complete(StatusCodes.OK -> "Welcome to raft world :)")
      } ~ path("init" / IntNumber) { number =>
        post {
          manager ! RaftManager.Initialize(number)
          complete(StatusCodes.Accepted -> s"Initializing raft with $number nodes")
        }
      } ~ path("kill" / IntNumber) { number =>
        post {
          manager ! RaftManager.KillNode(number)
          complete(StatusCodes.Accepted -> s"Killing node $number")
        }
      } ~ path("start" / IntNumber) { number =>
        post {
          manager ! RaftManager.StartNode(number)
          complete(StatusCodes.Accepted -> s"Attempt to start node $number")
        }
      }
//
//      path("example" / IntNumber) { id =>
//        post {
//          println(s"\n\nPOST IS WORKING id=$id\n\n")
//          complete(StatusCodes.Accepted -> "Request was accepted")
//        }
//      } ~ get {
//        complete(StatusCodes.OK -> "idzie idzie Podbeskidzie")
//      }
    }
  }

}

package pl.edu.agh.iosr.raft.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
  * @author lewap
  * @since 16.11.16
  */
class RaftController extends ErrorHandling {

  def routes: Route =
    handleExceptions(exceptionHandler)(endpoints)

  private def endpoints: Route = {
    pathPrefix("raft") {
      path("example" / IntNumber) { id =>
        post {
          println(s"\n\nPOST IS WORKING id=$id\n\n")
          complete(StatusCodes.Accepted -> "Request wa accepted")
        }
      }
    }
  }

}

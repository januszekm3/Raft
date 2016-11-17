package pl.edu.agh.iosr.raft.api

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction}
import pl.edu.agh.iosr.raft.ClientActor
import pl.edu.agh.iosr.raft.structure.ServerNode.InternalState

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
  * @author lewap
  * @since 16.11.16
  */
class RaftController extends ErrorHandling {

  implicit val system = ActorSystem("RaftHttp")
  implicit val formats = DefaultFormats
  implicit val timeout: Timeout = 5 seconds
  val manager = system.actorOf(RaftManager.props(), "RaftManager")

  def routes: Route =
    handleExceptions(exceptionHandler)(endpoints)

  private def endpoints: Route = {
    pathPrefix("raft") {
      path("state") {
        get {
          val futureResult: Future[List[InternalState]] = (manager ? RaftManager.GetState).mapTo[List[InternalState]]
          val result = Await.result(futureResult, 5 seconds) map { state =>
            NodeStateJsonSerializable(
              state.name, state.state.toString, state.otherNodes.map(_.name), state.leader.map(_.path.name),
              state.number, state.leaderRequestAcceptedCounter, state.lastSuccessfulCommitDate
            )
          }
          complete(StatusCodes.OK -> pretty(Extraction.decompose(result)))
        }
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
      } ~ path("set" / IntNumber) { number =>
        post {
          manager ! ClientActor.SetStateToRandomNode(number)
          complete(StatusCodes.Accepted -> s"Attempt to set $number")
        }
      } ~ get {
        complete(StatusCodes.OK -> "Welcome to raft world :)")
      }

    }
  }

}

case class NodeStateJsonSerializable(name: String,
                                     state: String,
                                     otherNodes: Set[String],
                                     leader: Option[String],
                                     number: Int,
                                     leaderRequestAcceptedCounter: Int,
                                     lastSuccessfulCommitDate: Option[Date])
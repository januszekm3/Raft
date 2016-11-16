package pl.edu.agh.iosr.raft.api


import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.typesafe.scalalogging.StrictLogging

trait ErrorHandling extends StrictLogging {

  val exceptionHandler = ExceptionHandler {
    case e: Exception =>
      extractUri { uri =>
        logger.error(s"Request to $uri could not be handled normally. Exception = $e")
        complete(HttpResponse(StatusCodes.InternalServerError))
      }
  }

}

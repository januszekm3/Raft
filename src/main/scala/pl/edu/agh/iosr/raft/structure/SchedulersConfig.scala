package pl.edu.agh.iosr.raft.structure

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

case class SchedulersConfig(initialHeartbeatDelay: FiniteDuration = 0 seconds,
                            heartbeatInterval: FiniteDuration = 80 millis,
                            timeout: FiniteDuration = 120 millis)

object SchedulersConfig {

  def random() =
    SchedulersConfig(
      initialHeartbeatDelay = 0 seconds,
      heartbeatInterval = (3 + Random.nextInt(3)) seconds,
      timeout = (3 + Random.nextDouble()) seconds
    )

}

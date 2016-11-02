package pl.edu.agh.iosr.raft.structure

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

case class SchedulersConfig(initialHeartbeatDelay: FiniteDuration,
                            heartbeatInterval: FiniteDuration,
                            timeout: FiniteDuration)

object SchedulersConfig {

  def random() =
    SchedulersConfig(
      initialHeartbeatDelay = 0 seconds,
      heartbeatInterval = (3 + Random.nextInt(2)) seconds,
      timeout = (2 + Random.nextDouble()) seconds
    )

}
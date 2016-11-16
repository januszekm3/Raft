package pl.edu.agh.iosr.raft

import com.typesafe.config.ConfigFactory

import scala.util.Try

object Settings {

  val config = ConfigFactory.load()

  val nodesQuantity = Try(config.getInt("raft.nodesQuantity")).getOrElse(5)

  val host = Try(config.getString("raft.api.host")).getOrElse("127.0.0.1")
  val port = Try(config.getInt("raft.api.port")).getOrElse(9000)

}

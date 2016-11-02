package pl.edu.agh.iosr.raft

import com.typesafe.config.ConfigFactory

import scala.util.Try

object Settings {

  val config = ConfigFactory.load()

  val nodesQuantity = Try(config.getInt("raft.nodesQuantity")).getOrElse(5)

}

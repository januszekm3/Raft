package pl.edu.agh.iosr.raft.structure

object State {

  sealed abstract class State

  case object Leader extends State

  case object Candidate extends State

  case object Follower extends State

}

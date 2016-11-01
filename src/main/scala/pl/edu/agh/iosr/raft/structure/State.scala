package pl.edu.agh.iosr.raft.structure

/**
  * @author lewap
  * @since 02.11.16
  */
object State {

  sealed abstract class State

  case object Leader extends State

  case object Candidate extends State

  case object Follower extends State

}

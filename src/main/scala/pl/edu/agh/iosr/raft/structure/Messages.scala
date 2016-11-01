package pl.edu.agh.iosr.raft.structure

import pl.edu.agh.iosr.raft.structure.State.State

/**
  * @author lewap
  * @since 02.11.16
  */
object Messages {

  case class ChangeState(newState: State)

}

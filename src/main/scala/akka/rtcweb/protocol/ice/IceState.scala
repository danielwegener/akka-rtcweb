package akka.rtcweb.protocol.ice

sealed trait IceState
object IceState {
  case object disconnected extends IceState
  case object gathering extends IceState
  case object connecting extends IceState
  case object connected extends IceState
  case object ready extends IceState
  case object failed extends IceState
}

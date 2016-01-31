package akka.rtcweb.protocol.ice

sealed trait IceGatheringState
object IceGatheringState {
  /** the object was just created, and no networking has occurred yet. */
  case object `new` extends IceGatheringState
  /** the ICE engine is in the process of gathering candidates for this RTCPeerConnection. */
  case object gathering extends IceGatheringState

  /** the ICE engine has completed gathering. Events such as adding a new interface or a new TURN server will cause the state to go back to gathering. */
  case object complete extends IceGatheringState
}

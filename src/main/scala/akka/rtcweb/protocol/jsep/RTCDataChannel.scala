package akka.rtcweb.protocol.jsep

import akka.actor.{ ActorRef, Actor }
import akka.rtcweb.protocol.jsep.RTCDataChannel.OnStateChange

sealed trait RTCDataChannelState
object RTCDataChannelState {

  /**
   * The user agent is attempting to establish the underlying data transport. This is the initial state of a RTCDataChannel object created with createDataChannel() .
   */
  case object connecting extends RTCDataChannelState

  /**
   * The underlying data transport is established and communication is possible. This is the initial state of a RTCDataChannel object dispatched as a part of a RTCDataChannelEvent .
   */
  case object open extends RTCDataChannelState

  /**
   * The procedure to close down the underlying data transport has started.
   */
  case object closing extends RTCDataChannelState

  /**
   * The underlying data transport has been closed or could not be established.
   */
  case object closed extends RTCDataChannelState
}

object RTCDataChannel {

  sealed trait RTCDataChannelMessage
  final case class OnStateChange(newState: RTCDataChannelState)

}

final class RTCDataChannel(peerConnectionRef: ActorRef, listener: ActorRef) extends Actor {

  var channelState: RTCDataChannelState = RTCDataChannelState.connecting

  override def receive: Receive = ???

  /** Tell the listener that we went south  */
  override def postStop() = {
    listener ! OnStateChange(RTCDataChannelState.closed)
  }

}

package akka.rtcweb.protocol.jsep

import akka.actor._
import akka.rtcweb.protocol.jsep.RTCDataChannel.OnStateChange
import akka.rtcweb.protocol.jsep.RTCPeerConnection.RTCDataChannelInit

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

  def props(dataChannelListener: ActorRef, config: RTCDataChannelInit) = Props(new RTCDataChannel(dataChannelListener, config))

}

final class RTCDataChannel private[jsep] (val listener: ActorRef, config: RTCDataChannelInit) extends Actor with ActorLogging {

  val peerConnection = context.parent
  var channelState: RTCDataChannelState = RTCDataChannelState.connecting

  context.watch(listener)

  override def receive: Receive = {
    case Terminated(`listener`) => log.info("listener stopped. stopping self."); context.stop(self)
  }

  /** Tell the listener that we went south  */
  override def postStop() = {
    listener ! OnStateChange(RTCDataChannelState.closed)
  }

}

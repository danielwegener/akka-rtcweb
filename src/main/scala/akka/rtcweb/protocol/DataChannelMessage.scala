package akka.rtcweb.protocol

sealed class MessageType(val raw: Int)

object DATA_CHANNEL_OPEN extends MessageType(0x03)
object DATA_CHANNEL_ACK extends MessageType(0x02)

sealed class ChannelType(val raw: Int)

/**
 * The Data Channel provides a
 * reliable in-order bi-directional communication.
 */
object DATA_CHANNEL_RELIABLE extends ChannelType(0x00)

/**
 * The Data Channel provides
 * a reliable unordered bi-directional communication.
 */
object DATA_CHANNEL_RELIABLE_UNORDERED extends ChannelType(0x80)

/**
 * The Data Channel
 * provides a partially-reliable in-order bi-directional
 * communication.  User messages will not be retransmitted more
 * times than specified in the Reliability Parameter.
 */
object DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT extends ChannelType(0x01)

/**
 * The Data Channel
 * provides a partial reliable in-order bi-directional
 * communication.  User messages might not be transmitted or
 * retransmitted after a specified life-time given in milliseconds in the Reliability Parameter.  This life-time starts
 * when providing the user message to the protocol stack.
 */
object DATA_CHANNEL_PARTIAL_RELIABLE_TIMED extends ChannelType(0x02)

/**
 * The Data
 * Channel provides a partial reliable unordered bi-directional
 * communication.  User messages might not be transmitted or
 * retransmitted after a specified life-time given in milli-
 * seconds in the Reliability Parameter.  This life-time starts
 * when providing the user message to the protocol stack.
 */
object DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED extends ChannelType(0x82)

sealed trait DataChannelMessage {

  val messageType: MessageType
  val channelType: ChannelType
  val priority: Short
  val label: String
  /**
   * See WebSocket Subprotocol Name Registry [RFC6455]
   */
  val protocol: String

}

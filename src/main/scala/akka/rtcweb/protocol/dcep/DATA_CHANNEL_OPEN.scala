package akka.rtcweb.protocol.dcep

import scodec._
import scodec.codecs._

/**
 *  {{{
 * +-------------------+-----------+-----------+
   | Name              | Type      | Reference |
   +-------------------+-----------+-----------+
   | Reserved          | 0x00      | [RFCXXXX] |
   | Reserved          | 0x01      | [RFCXXXX] |
   | DATA_CHANNEL_ACK  | 0x02      | [RFCXXXX] |
   | DATA_CHANNEL_OPEN | 0x03      | [RFCXXXX] |
   | Unassigned        | 0x04-0xfe |           |
   | Reserved          | 0xff      | [RFCXXXX] |
   +-------------------+-----------+-----------+
}}}
 */
sealed trait `Message Type`
object `Message Type` {

  case object DATA_CHANNEL_ACK extends `Message Type`
  case object DATA_CHANNEL_OPEN  extends `Message Type`

  implicit val codec = "Message Type" | mappedEnum(uint8,
    DATA_CHANNEL_ACK -> 0x01,
    DATA_CHANNEL_OPEN -> 0x02)
}

sealed trait `Channel Type`
object `Channel Type` {

  /**
   * (0x00):  The Data Channel provides a
      reliable in-order bi-directional communication.
   */
  case object DATA_CHANNEL_RELIABLE extends `Channel Type`

  /**
   *  (0x80):  The Data Channel provides
    a reliable unordered bi-directional communication.
   */
  case object DATA_CHANNEL_RELIABLE_UNORDERED extends `Channel Type`

  /**
   *  (0x01):  The Data Channel
  provides a partially-reliable in-order bi-directional
  communication.  User messages will not be retransmitted more
  times than specified in the Reliability Parameter.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT extends `Channel Type`

  /**
   * (0x81):  The Data
    Channel provides a partial reliable unordered bi-directional
  communication.  User messages will not be retransmitted more
  times than specified in the Reliability Parameter.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED extends `Channel Type`

  /**
   * (0x02):  The Data Channel
  provides a partial reliable in-order bi-directional
  communication.  User messages might not be transmitted or
  retransmitted after a specified life-time given in milliseconds in the Reliability Parameter.
  This life-time starts when providing the user message to the protocol stack.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_TIMED extends `Channel Type`

  /**
   * (0x82):  The Data
    Channel provides a partial reliable unordered bi-directional
  communication.  User messages might not be transmitted or
  retransmitted after a specified life-time given in milli-
    seconds in the Reliability Parameter.  This life-time starts
    when providing the user message to the protocol stack.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED  extends `Channel Type`

  implicit val codec = "Channel Type" | mappedEnum(uint8,
    DATA_CHANNEL_RELIABLE -> 0x00,
    DATA_CHANNEL_RELIABLE_UNORDERED -> 0x80,
    DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT -> 0x01,
    DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED -> 0x81,
    DATA_CHANNEL_PARTIAL_RELIABLE_TIMED -> 0x02,
    DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED -> 0x82

  )
}


/**
 * This message is sent initially on the Stream used for user messages
   using the Data Channel.

 * @param messageType This field holds the IANA defined message type for the
      DATA_CHANNEL_OPEN message.  The value of this field is 0x03 as
      specified in Section 8.2.1.
 */
case class DATA_CHANNEL_OPEN(
  messageType: `Message Type`

)

object DATA_CHANNEL_OPEN {

  implicit val codec = "DATA_CHANNEL_OPEN" | {

  }
}

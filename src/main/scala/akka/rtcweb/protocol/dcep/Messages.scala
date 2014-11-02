package akka.rtcweb.protocol.dcep

import scodec._
import scodec.codecs._
import akka.rtcweb.protocol.scodec.SCodecContrib._
import scalaz._

/**
 *  {{{
 * +-------------------+-----------+-----------+
 * | Name              | Type      | Reference |
 * +-------------------+-----------+-----------+
 * | Reserved          | 0x00      | [RFCXXXX] |
 * | Reserved          | 0x01      | [RFCXXXX] |
 * | DATA_CHANNEL_ACK  | 0x02      | [RFCXXXX] |
 * | DATA_CHANNEL_OPEN | 0x03      | [RFCXXXX] |
 * | Unassigned        | 0x04-0xfe |           |
 * | Reserved          | 0xff      | [RFCXXXX] |
 * +-------------------+-----------+-----------+
 * }}}
 */
private[dcep] sealed trait `Message Type`
private[dcep] object `Message Type` {

  case object DATA_CHANNEL_ACK extends `Message Type`
  case object DATA_CHANNEL_OPEN extends `Message Type`

  implicit val codec = "Message Type" | mappedEnum(uint8,
    DATA_CHANNEL_ACK -> 0x01,
    DATA_CHANNEL_OPEN -> 0x02)
}

private[dcep] sealed trait `Channel Type`
private[dcep] object `Channel Type` {

  /**
   * (0x00):  The Data Channel provides a
   * reliable in-order bi-directional communication.
   */
  case object DATA_CHANNEL_RELIABLE extends `Channel Type`

  /**
   *  (0x80):  The Data Channel provides
   * a reliable unordered bi-directional communication.
   */
  case object DATA_CHANNEL_RELIABLE_UNORDERED extends `Channel Type`

  /**
   *  (0x01):  The Data Channel
   * provides a partially-reliable in-order bi-directional
   * communication.  User messages will not be retransmitted more
   * times than specified in the Reliability Parameter.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT extends `Channel Type`

  /**
   * (0x81):  The Data
   * Channel provides a partial reliable unordered bi-directional
   * communication.  User messages will not be retransmitted more
   * times than specified in the Reliability Parameter.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED extends `Channel Type`

  /**
   * (0x02):  The Data Channel
   * provides a partial reliable in-order bi-directional
   * communication.  User messages might not be transmitted or
   * retransmitted after a specified life-time given in milliseconds in the Reliability Parameter.
   * This life-time starts when providing the user message to the protocol stack.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_TIMED extends `Channel Type`

  /**
   * (0x82):  The Data
   * Channel provides a partial reliable unordered bi-directional
   * communication.  User messages might not be transmitted or
   * retransmitted after a specified life-time given in milli-
   * seconds in the Reliability Parameter.  This life-time starts
   * when providing the user message to the protocol stack.
   */
  case object DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED extends `Channel Type`

  implicit val codec: Codec[`Channel Type`] = "Channel Type" | mappedEnum(uint8,
    DATA_CHANNEL_RELIABLE -> 0x00,
    DATA_CHANNEL_RELIABLE_UNORDERED -> 0x80,
    DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT -> 0x01,
    DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED -> 0x81,
    DATA_CHANNEL_PARTIAL_RELIABLE_TIMED -> 0x02,
    DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED -> 0x82

  )
}

/**
 * For use in WebRTC, the values used
 * SHOULD be one of 128 ("below normal"), 256 ("normal"), 512 ("high")
 * or 1024 ("extra high").
 */
private[dcep] sealed trait Priority //todo: extends Ordered[Priority]

private[dcep] object Priority {
  case object `below normal` extends Priority
  case object `normal` extends Priority
  case object `high` extends Priority
  case object `extra high` extends Priority
  final case class Ordinal(value: Int) extends Priority

  implicit val codec: Codec[Priority] = "Priority" | choice(mappedEnum[Priority, Int](uint16,
    `below normal` -> 128,
    `normal` -> 256,
    `high` -> 256,
    `extra high` -> 1024
  ),
    uint16.as[Ordinal].widen[Priority](identity, {
      case a: Ordinal => \/-(a)
      case _ => -\/(Err("Is not an Ordinal"))
    })
  )

}

/**
 * This message is sent initially on the Stream used for user messages
 * using the Data Channel.
 *
 * param messageType This field holds the IANA defined message type for the
 * DATA_CHANNEL_OPEN message.  The value of this field is 0x03 as
 * specified in Section 8.2.1.
 * @param channelType This field specifies the type of the Data Channel to be opened and
 * the values are managed by IANA (see Section 8.2.2):
 * @param priority The priority of the Data Channel as described in
 * [I-D.ietf-rtcweb-data-channel].
 * @param reliability For reliable Data Channels this field MUST be set to 0 on the
 * sending side and MUST be ignored on the receiving side.  If a
 * partial reliable Data Channel with limited number of
 * retransmissions is used, this field specifies the number of
 * retransmissions.  If a partial reliable Data Channel with limited
 * lifetime is used, this field specifies the maximum lifetime in
 * milliseconds.  The following table summarizes this:
 */
private[dcep] final case class DATA_CHANNEL_OPEN(
  channelType: `Channel Type`,
  priority: Priority,
  reliability: Long,
  label: String,
  protocol: String)

private[dcep] object DATA_CHANNEL_OPEN {

  /**
   * {{{
   *   0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |  Message Type |  Channel Type |            Priority           |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                    Reliability Parameter                      |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |         Label Length          |       Protocol Length         |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               /
   * |                             Label                             |
   * /                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               /
   * |                            Protocol                           |
   * /                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec = "DATA_CHANNEL_OPEN" | {
    ("Message Type" | constantValue(`Message Type`.DATA_CHANNEL_OPEN)) :~>:
      ("Channel Type" | `Channel Type`.codec) ::
      ("Priority" | Priority.codec) ::
      ("Reliability" | uint32) :: variableSizeBytes2[String, String](
        "Label Length" | uint8, "Protocl Length" | uint8,
        "Label" | ascii, "Protocol" | ascii
      )
  }.as[DATA_CHANNEL_OPEN]
}

private[dcep] case object DATA_CHANNEL_ACK {
  implicit val codec = "DATA_CHANNEL_ACK" | constantValue(`Message Type`.DATA_CHANNEL_ACK) :: provide(DATA_CHANNEL_ACK)
}
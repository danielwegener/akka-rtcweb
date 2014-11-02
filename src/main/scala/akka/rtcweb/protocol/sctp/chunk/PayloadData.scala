package akka.rtcweb.protocol.sctp.chunk

import akka.rtcweb.protocol.sctp.chunk.PayloadData.PayloadProtocolIdentifier
import scodec.Codec
import scodec.bits.ByteVector
import scodec.Err
import scodec.codecs._
import scalaz.{\/-, -\/}

private[sctp] object PayloadData {

  /**
   * The following format MUST be used for the DATA chunk:
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Type = 0    | Reserved|U|B|E|    Length                     |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                              TSN                              |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |      Stream Identifier S      |   Stream Sequence Number n    |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                  Payload Protocol Identifier                  |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /                 User Data (seq n of Stream S)                 /
   * \                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec: Codec[PayloadData] = {
    constant(ChunkType.codec.encodeValid(ChunkType.DATA)) ~>
      ignore(5) ~>
      ("U bit" | bool) ::
      ("B bit" | bool) ::
      ("E bit" | bool) ::
      variableSizeBytes("length" | uint16,
        ("TSN" | uint32) ::
        ("Stream Identifier S" | uint16) ::
        ("Stream Sequence Number n" | uint16) ::
        ("Payload Protocol Identifier" | PayloadProtocolIdentifier.codec) ::
        ("User Data" | bytes)
      , 4)
  }.as[PayloadData]

  sealed trait PayloadProtocolIdentifier

  object PayloadProtocolIdentifier {

    final case class Unspecified(raw: Int) extends PayloadProtocolIdentifier
    case object `WebRTC String` extends PayloadProtocolIdentifier
    case object `WebRTC Binary` extends PayloadProtocolIdentifier
    case object `WebRTC String Empty` extends PayloadProtocolIdentifier
    case object `WebRTC Binary Empty` extends PayloadProtocolIdentifier


    /** @see [[https://tools.ietf.org/html/draft-ietf-rtcweb-data-protocol-08#section-8.1 draft-ietf-rtcweb-data-protocol-08: SCTP Payload Protocol Identifier]] */
    case object `WebRTC DCEP` extends PayloadProtocolIdentifier

    implicit val codec: Codec[PayloadProtocolIdentifier] = choice(mappedEnum(uint8,
      `WebRTC DCEP` -> 50,
      `WebRTC String` -> 51,
      `WebRTC Binary` -> 53,
      `WebRTC String Empty` -> 56,
      `WebRTC Binary Empty` -> 57
    ), uint8.as[Unspecified].widen(identity, {
      case b:Unspecified => \/-(b)
      case b => -\/(Err(s"$b is not Unspecified"))
    })
    )

  }

}

/**
 * @param unordered The (U)nordered bit, if set to '1', indicates that this is an
 * unordered DATA chunk, and there is no Stream Sequence Number
 * assigned to this DATA chunk.  Therefore, the receiver MUST ignore
 * the Stream Sequence Number field.<br />
 * After reassembly (if necessary), unordered DATA chunks MUST be
 * dispatched to the upper layer by the receiver without any attempt
 * to reorder.<br />
 * If an unordered user message is fragmented, each fragment of the
 * message MUST have its U bit set to '1'.
 * @param beginning The (B)eginning fragment bit, if set, indicates the first fragment
 * of a user message.
 * @param ending The (E)nding fragment bit, if set, indicates the last fragment of
 * a user message.
 * @param tsn  This value represents the TSN for this DATA chunk.  The valid
 * range of TSN is from 0 to 4294967295 (2**32 - 1).  TSN wraps back
 * to 0 after reaching 4294967295.
 * @param streamIdentifier Identifies the stream to which the following user data belongs.
 * @param streamSequenceNumber This value represents the Stream Sequence Number of the following
 * user data within the stream S.  Valid range is 0 to 65535.
 * @param payloadProtocolIdentifier This value represents an application (or upper layer) specified
 * protocol identifier.  This value is passed to SCTP by its upper
 * layer and sent to its peer.  This identifier is not used by SCTP
 * but can be used by certain network entities, as well as by the
 * peer application, to identify the type of information being
 * carried in this DATA chunk.  This field must be sent even in
 * fragmented DATA chunks (to make sure it is available for agents in
 * the middle of the network).  Note that this field is NOT touched
 * by an SCTP implementation; therefore, its byte order is NOT
 * necessarily big endian.  The upper layer is responsible for any
 * byte order conversions to this field.<br />
 * The value 0 indicates that no application identifier is specified
 * by the upper layer for this payload data.
 * @param userData  This is the payload user data.  The implementation MUST pad the
 * end of the data to a 4-byte boundary with all-zero bytes.  Any
 * padding MUST NOT be included in the Length field.  A sender MUST
 * never add more than 3 bytes of padding.
 */
private[sctp] final case class PayloadData(
  unordered: Boolean,
  beginning: Boolean,
  ending: Boolean,
  tsn: Long,
  streamIdentifier: Int,
  streamSequenceNumber: Int,
  payloadProtocolIdentifier: PayloadProtocolIdentifier,
  userData: ByteVector) extends SctpChunk
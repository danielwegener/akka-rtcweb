package akka.rtcweb.protocol.sctp.chunk

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

object PayloadData {

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
    constant(uint8.encodeValid(0)) ~>
      ignore(5) ~>
      ("U bit" | bool) ::
      ("B bit" | bool) ::
      ("E bit" | bool) ::
      ("length" | uint16) ::
      ("TSN" | uint32) ::
      ("Stream Identifier S" | uint16) ::
      ("Stream Sequence Number n" | uint16) ::
      ("Payload Protocol Identifier" | uint32) ::
      ("User Data" | bytes(???))
  }.as[PayloadData]
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
 * @param length This field indicates the length of the DATA chunk in bytes from
 * the beginning of the type field to the end of the User Data field
 * excluding any padding.  A DATA chunk with one byte of user data
 * will have Length set to 17 (indicating 17 bytes).<br />
 * A DATA chunk with a User Data field of length L will have the
 * Length field set to (16 + L) (indicating 16+L bytes) where L MUST
 * be greater than 0.
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
final case class PayloadData(
  unordered: Boolean,
  beginning: Boolean,
  ending: Boolean,
  length: Int,
  tsn: Long,
  streamIdentifier: Int,
  streamSequenceNumber: Int,
  payloadProtocolIdentifier: Long,
  userData: ByteVector) extends SctpChunk
package akka.rtcweb.protocol.sctp.chunk

import scodec._
import scodec.codecs._
import scodec.bits._
import shapeless.ops.tuple.FlatMapper
import akka.rtcweb.protocol.scodec.SCodecContrib._
import akka.rtcweb.protocol.scodec.ShapelessContrib._

object Initiation {

  sealed trait InitiationParameterType

  object InitiationParameterType {

    case object `IPv4 Address Parameter` extends InitiationParameterType

    implicit val codec = mappedEnum(uint8,
      `IPv4 Address Parameter` -> 5
    )

  }

  case class IPv4Address(raw: Int) extends AnyVal

  object IPv4Address {
    implicit val codec = uint8.as[IPv4Address]
  }

  sealed trait InitiationParameter extends Parameter

  final case class `IPv4 Address Parameter`(address: IPv4Address) extends InitiationParameter

  object `IPv4 Address Parameter` {
    /**
     * {{{
     *   0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |        Type = 5               |      Length = 8               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                        IPv4 Address                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * }}}
     */
    implicit val codec: Codec[`IPv4 Address Parameter`] = {
      ("Type" | constant(uint8.encodeValid(5))) ~>
        ("Length" | constant(uint8.encodeValid(8))) ~>
        ("IPv4 Address" | IPv4Address.codec)
    }.as[`IPv4 Address Parameter`]
  }

  /**
   * This chunk is used to initiate an SCTP association between two
   * endpoints.  The format of the INIT chunk is shown below:
   *
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Type = 1    |  Chunk Flags  |      Chunk Length             |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                         Initiate Tag                          |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |           Advertised Receiver Window Credit (a_rwnd)          |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |  Number of Outbound Streams   |  Number of Inbound Streams    |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                          Initial TSN                          |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /              Optional/Variable-Length Parameters              /
   * \                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec: Codec[Initiation] = {
    /*constant(uint8.encodeValid(1)) ~>
      ignore(8) ~>
      ("length" | variableSizeBytes(uint16, {
        ("Initiate Tag" | nonZero(uint32) ) ~
          ("Advertised Receiver Window Credit" | uint32) ~
          ("Number of Outbound Streams" | nonZero(uint16) ) ~
          ("Number of Inbound Streams" | nonZero(uint16) ) ~
          ("Initial TSN" | uint32) ~
          ("Optional Parameters" | vector(UnrecognizedParameter.codec))
      }, 4))*/
    "Initiation" | {
      constant(uint8.encodeValid(1)) ~>
        ignore(8) ~>
        ("length" | uint16) >>:~ { length: Int =>
          ("Initiate Tag" | nonZero(uint32)) ::
            ("Advertised Receiver Window Credit" | uint32) ::
            ("Number of Outbound Streams" | nonZero(uint16)) ::
            ("Number of Inbound Streams" | nonZero(uint16)) ::
            ("Initial TSN" | uint32) ::
            ("Optional Parameters" | fixedSizeBytes(length, vector(UnrecognizedParameter.codec)))
        }
    }.as[Initiation]

  }

}

/**
 *
 * @param initiateTag The receiver of the INIT (the responding end) records the value of
 * the Initiate Tag parameter.  This value MUST be placed into the
 * Verification Tag field of every SCTP packet that the receiver of
 * the INIT transmits within this association.<br />
 * The Initiate Tag is allowed to have any value except 0.  See
 * Section 5.3.1 for more on the selection of the tag value.<br />
 * If the value of the Initiate Tag in a received INIT chunk is found
 * to be 0, the receiver MUST treat it as an error and close the
 * association by transmitting an ABORT.
 * @param advertisedReceiverWindowCredit This value represents the dedicated buffer space, in number of
 * bytes, the sender of the INIT has reserved in association with this window.
 * During the life of the association, this buffer space SHOULD NOT be lessened (i.e., dedicated buffers taken away
 * from this association); however, an endpoint MAY change the value
 * of a_rwnd it sends in SACK chunks.
 * @param numberOfOutboundStreams  Defines the number of outbound streams the sender of this INIT
 * chunk wishes to create in this association.  The value of 0 MUST
 * NOT be used.<br/>
 * Note: A receiver of an INIT with the OS value set to 0 SHOULD
 * abort the association.
 * @param numberOfInboundStreams Defines the maximum number of streams the sender of this INIT
 * chunk allows the peer end to create in this association.  The
 * value 0 MUST NOT be used.<br/>
 * Note: There is no negotiation of the actual number of streams but
 * instead the two endpoints will use the min(requested, offered).
 * See Section 5.1.1 for details.<br/>
 * Note: A receiver of an INIT with the MIS value of 0 SHOULD abort
 * the association.
 * @param initialTsn Defines the initial TSN that the sender will use.  The valid range
 * is from 0 to 4294967295.  This field MAY be set to the value of
 * the Initiate Tag field.
 */
final case class Initiation(
  length: Int,
  initiateTag: Long,
  advertisedReceiverWindowCredit: Long,
  numberOfOutboundStreams: Int,
  numberOfInboundStreams: Int,
  initialTsn: Long,
  optionalParameters: Vector[UnrecognizedParameter]) extends SctpChunk

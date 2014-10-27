package akka.rtcweb.protocol.sctp.chunk

import akka.rtcweb.protocol.sctp.chunk.Initiation.InitiationParameter
import akka.rtcweb.protocol.sdp.TimeUnit
import scodec._
import scodec.bits.{ ByteVector }
import scodec.codecs._
import akka.rtcweb.protocol.scodec.SCodecContrib._
import concurrent.duration._

private[sctp] object Initiation {

  sealed trait InitiationParameterType

  object InitiationParameterType {

    case object `IPv4 Address Parameter` extends InitiationParameterType

    implicit val codec = mappedEnum(uint8,
      `IPv4 Address Parameter` -> 5
    )

  }

  sealed trait InitiationParameter extends Parameter

  final case class `IPv4 Address`(raw: ByteVector) extends InitiationParameter
  object `IPv4 Address` {
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
    implicit val codec: Codec[`IPv4 Address`] = {
      "IPv4 Address" |
        ("Type" | constant(uint8.encodeValid(5))) ~>
        ("Length" | constant(uint8.encodeValid(8))) ~>
        ("IPv4 Address" | bytes(4))
    }.as[`IPv4 Address`]
  }

  final case class `IPv6 Address`(raw: ByteVector) extends InitiationParameter
  object `IPv6 Address` {
    /**
     * {{{
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Type = 6           |          Length = 20          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * |                         IPv6 Address                          |
     * |                                                               |
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * }}}
     */
    implicit val codec: Codec[`IPv6 Address`] = {
      "IPv6 Address" |
        ("Type" | constant(uint8.encodeValid(6))) ~>
        ("Length" | constant(uint8.encodeValid(20))) ~>
        ("IPv6 Address" | bytes(16))
    }.as[`IPv6 Address`]
  }

  final case class `Cookie Preservative`(lifeSpan: FiniteDuration) extends InitiationParameter
  object `Cookie Preservative` {
    /**
     * {{{
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |          Type = 9             |          Length = 8           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |         Suggested Cookie Life-Span Increment (msec.)          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * }}}
     */
    implicit val codec: Codec[`Cookie Preservative`] = {
      "Cookie Preservative" |
        ("Type" | constant(uint8.encodeValid(9))) ~>
        ("Length" | constant(uint8.encodeValid(8))) ~>
        ("Suggested Cookie Life-Span Increment" | duration(uint32, MILLISECONDS))
    }.as[`Cookie Preservative`]
  }

  final case class `Host Name Address`(hostName: String) extends InitiationParameter
  object `Host Name Address` {
    /**
     * {{{
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |          Type = 11            |          Length               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * /                          Host Name                            /
     * \                                                               \
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * }}}
     */
    implicit val codec: Codec[`Host Name Address`] = "Host Name Address" | {
      ("Type" | constant(uint8.encodeValid(11))) :~>:
        variableSizeBytes("Length" | uint8, "Host Name" | ascii.cstring, 4)
    }.as[`Host Name Address`]
  }

  sealed trait `Address Type`
  object `Address Type` {
    case object IPv4 extends `Address Type`
    case object IPv6 extends `Address Type`
    case object `Host Name` extends `Address Type`

    implicit val codec: Codec[`Address Type`] = "Address Type" | mappedEnum(uint8,
      IPv4 -> 5,
      IPv6 -> 6,
      `Host Name` -> 11
    )
  }

  final case class `Supported Address Types`(addressTypes: Vector[`Address Type`]) extends InitiationParameter

  object `Supported Address Types` {

    /**
     *         0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |          Type = 12            |          Length               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |        Address Type #1        |        Address Type #2        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                            ......                             |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+
     */
    implicit val codec: Codec[`Supported Address Types`] = "Supported Address Types" | {
      ("Type" | constant(uint8.encodeValid(12))) :~>:
        variableSizeBytes("Length" | uint8,
          "Address Type" | vector(`Address Type`.codec),
          4)
    }.as[`Supported Address Types`]

  }

  /**
   * Codec that parses all supported optional parameters
   */
  val parameterCodec: Codec[InitiationParameter] = Codec.coproduct[InitiationParameter].choice

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
    "Initiation" | {
      constant(uint8.encodeValid(1)) :~>:
        ignore(8) :~>:
        variableSizeBytes("Chunk Length" | uint16,
          ("Initiate Tag" | nonZero(uint32)) ::
            ("Advertised Receiver Window Credit" | uint32) ::
            ("Number of Outbound Streams" | nonZero(uint16)) ::
            ("Number of Inbound Streams" | nonZero(uint16)) ::
            ("Initial TSN" | uint32) ::
            ("Optional Parameters" | vector(parameterCodec)), 2)

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
private[sctp] final case class Initiation(
  initiateTag: Long,
  advertisedReceiverWindowCredit: Long,
  numberOfOutboundStreams: Int,
  numberOfInboundStreams: Int,
  initialTsn: Long,
  optionalParameters: Vector[InitiationParameter]) extends SctpChunk

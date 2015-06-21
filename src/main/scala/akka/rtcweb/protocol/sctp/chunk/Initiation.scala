package akka.rtcweb.protocol.sctp.chunk

import akka.rtcweb.protocol.sctp.chunk.Initiation.InitiationParameter
import scodec._
import scodec.bits.ByteVector
import scodec.codecs._
import akka.rtcweb.protocol.scodec.SCodecContrib._
import shapeless._
import concurrent.duration._

private[sctp] object Initiation {

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
      constant(ChunkType.codec.encode(ChunkType.INIT).require) :~>:
        ignore(8) :~>:
        variableSizeBytes("Chunk Length" | uint16,
          ("Initiate Tag" | nonZero(uint32)) ::
            ("Advertised Receiver Window Credit" | uint32) ::
            ("Number of Outbound Streams" | nonZero(uint16)) ::
            ("Number of Inbound Streams" | nonZero(uint16)) ::
            ("Initial TSN" | uint32) ::
            ("Optional Parameters" | vector(InitiationParameter.codec)), 2)

    }.as[Initiation]

  }

  /**
   * Codec that parses all supported optional parameters
   */

  sealed trait InitiationParameter

  sealed trait `Address Type`

  /**
   *  Contains an IPv4 address of the sending endpoint.  It is binary
   * encoded.
   */
  final case class `IPv4 Address`(raw: ByteVector) extends InitiationParameter

  /**
   * Contains an IPv6 address of the sending endpoint.  It is binary
   * encoded.
   *
   * Contains an IPv6 address of the sending endpoint.  It is binary
   * encoded.
   *
   * Note: A sender MUST NOT use an IPv4-mapped IPv6 address [RFC2373]
   * but should instead use an IPv4 Address Parameter for an IPv4
   * address.
   *
   * Combined with the Source Port Number in the SCTP common header,
   * the value passed in an IPv4 or IPv6 Address parameter indicates a
   * transport address the sender of the INIT will support for the
   * association being initiated.  That is, during the lifetime of this
   * association, this IP address can appear in the source address
   * field of an IP datagram sent from the sender of the INIT, and can
   * be used as a destination address of an IP datagram sent from the
   * receiver of the INIT.
   *
   * More than one IP Address parameter can be included in an INIT
   * chunk when the INIT sender is multi-homed.  Moreover, a multi-
   * homed endpoint may have access to different types of network, thus
   * more than one address type can be present in one INIT chunk, i.e.,
   * IPv4 and IPv6 addresses are allowed in the same INIT chunk.
   *
   * If the INIT contains at least one IP Address parameter, then the
   * source address of the IP datagram containing the INIT chunk and
   * any additional address(es) provided within the INIT can be used as
   * destinations by the endpoint receiving the INIT.  If the INIT does
   * not contain any IP Address parameters, the endpoint receiving the
   * INIT MUST use the source address associated with the received IP
   * datagram as its sole destination address for the association.
   *
   * Note that not using any IP address parameters in the INIT and
   * INIT-ACK is an alternative to make an association more likely to
   * work across a NAT box.
   */
  final case class `IPv6 Address`(raw: ByteVector) extends InitiationParameter

  /**
   * The sender of the INIT shall use this parameter to suggest to the
   * receiver of the INIT for a longer life-span of the State Cookie.
   *
   * This parameter indicates to the receiver how much increment in
   * milliseconds the sender wishes the receiver to add to its default
   * cookie life-span.
   *
   * This optional parameter should be added to the INIT chunk by the
   * sender when it re-attempts establishing an association with a peer
   * to which its previous attempt of establishing the association failed
   * due to a stale cookie operation error.  The receiver MAY choose to
   * ignore the suggested cookie life-span increase for its own security
   * reasons.
   *
   */
  final case class `Cookie Preservative`(lifeSpan: FiniteDuration) extends InitiationParameter

  /**
   * The sender of INIT uses this parameter to pass its Host Name (in
   * place of its IP addresses) to its peer.  The peer is responsible
   * for resolving the name.  Using this parameter might make it more
   * likely for the association to work across a NAT box.
   * @param hostName  This field contains a host name in "host name syntax" per RFC1123
   * Section 2.1 [RFC1123].  The method for resolving the host name is
   * out of scope of SCTP.
   */
  final case class `Host Name Address`(hostName: String) extends InitiationParameter

  /**
   * This parameter is used to pad an INIT chunk.  A PAD parameter can be
   * used to enlarge the INIT chunk by 4 bytes as the minimum to the
   * maximum size of the INIT chunk in steps of 4 bytes.  An INIT chunk
   * MAY contain multiple PAD parameters.
   * @see [[https://tools.ietf.org/html/rfc4820#section-4 RFC4820: Padding Parameter (PAD)]]
   * @param length This value holds the length of the Padding Data plus 4.
   */
  final case class `Padding Parameter`(length: Int) extends InitiationParameter

  final case class `Supported Address Types`(addressTypes: Vector[`Address Type`]) extends InitiationParameter

  /**
   * At the initialization of the association, the sender of the INIT or
   * INIT ACK chunk MAY include this OPTIONAL parameter to inform its peer
   * that it is able to support the Forward TSN chunk (see Section 3.3 for
   * further details).
   * @see [[https://tools.ietf.org/html/rfc3758#section-3.1 rfc3758 Protocol Changes to support PR-SCTP]]
   */
  final case class `Forward-TSN-Supported`() extends InitiationParameter

  /**
   *  This parameter value MUST contain all the necessary state and
   * parameter information required for the sender of this INIT ACK to
   * create the association, along with a Message Authentication Code
   * (MAC).  See Section 5.1.3 for details on State Cookie definition.
   */
  final case class `State Cookie`(data: ByteVector) extends InitiationParameter

  final case class `Unrecognized Parameter`(unrecognizedParameters: Vector[InitiationParameter])
    extends InitiationParameter

  object `IPv4 Address` {
    implicit val discriminator: Discriminator[InitiationParameter, `IPv4 Address`, Int] = Discriminator(5)

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
        ("Length" | constant(uint16.encode(8).require)) ~>
        ("IPv4 Address" | bytes(4))
    }.as[`IPv4 Address`]
  }

  object `IPv6 Address` {
    implicit val discriminator: Discriminator[InitiationParameter, `IPv6 Address`, Int] = Discriminator(6)
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
        ("Length" | constant(uint16.encode(20).require)) ~>
        ("IPv6 Address" | bytes(16))
    }.as[`IPv6 Address`]
  }

  object `Cookie Preservative` {
    implicit val discriminator: Discriminator[InitiationParameter, `Cookie Preservative`, Int] = Discriminator(9)
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
      ("Length" | constant(uint16.encode(8).require)) ~>
        ("Suggested Cookie Life-Span Increment" | duration(uint32, MILLISECONDS))
    }.as[`Cookie Preservative`]
  }

  object `Host Name Address` {
    implicit val discriminator: Discriminator[InitiationParameter, `Host Name Address`, Int] = Discriminator(11)

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
     * Note: At least one null terminator is included in the Host Name
     * string and must be included in the length.
     * }}}
     */
    implicit val codec: Codec[`Host Name Address`] = {
      variableSizeBytes("Length" | uint16, "Host Name" | ascii.cstring, 4)
    }.as[`Host Name Address`]
  }

  object `Padding Parameter` {
    implicit val discriminator: Discriminator[InitiationParameter, `Padding Parameter`, Int] = Discriminator(0x8005)
    /**
     * {{{
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |     Parameter Type = 0x8005   |       Parameter Length        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * /                                                               /
     * \                          Padding Data                         \
     * /                                                               /
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * }}}
     */
    implicit val codec: Codec[`Padding Parameter`] = {
      ("Parameter Length" | uint16) >>:~ { length =>
        ("Padding Data" | ignore(length * 8 - 4 * 8)).hlist
      }
    }.dropUnits.as[`Padding Parameter`]
  }
  object `Forward-TSN-Supported` {
    implicit val discriminator: Discriminator[InitiationParameter, `Forward-TSN-Supported`, Int] = Discriminator(49152)
    /**
     * {{{
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    Parameter Type = 49152     |  Parameter Length = 4         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * }}}
     */
    implicit val codec: Codec[`Forward-TSN-Supported`.type] = {
      ("Parameter Length" | constant(uint16.encode(4).require)) ~>
        provide(`Forward-TSN-Supported`)
    }

  }

  object `Address Type` {
    implicit val codec: Codec[`Address Type`] = "Address Type" | mappedEnum(uint16,
      IPv4 -> 5,
      IPv6 -> 6,
      `Host Name` -> 11
    )
    case object IPv4 extends `Address Type`
    case object IPv6 extends `Address Type`
    case object `Host Name` extends `Address Type`
  }

  object `Supported Address Types` {

    implicit val discriminator: Discriminator[InitiationParameter, `Supported Address Types`, Int] = Discriminator(12)

    /**
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |          Type = 12            |          Length               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |        Address Type #1        |        Address Type #2        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                            ......                             |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+
     */
    implicit val codec: Codec[`Supported Address Types`] = {
      variableSizeBytes("Length" | uint16,
        "Address Type" | vector(`Address Type`.codec),
        4)
    }.as[`Supported Address Types`]
  }

  object `State Cookie` {
    implicit val discriminator: Discriminator[InitiationParameter, `State Cookie`, Int] = Discriminator(7)

    implicit val codec: Codec[`State Cookie`] = {
      variableSizeBytes("Length" | uint16,
        "Parameter Value" | bytes,
        4)
    }.as[`State Cookie`]
  }

  object `Unrecognized Parameter` {

    implicit val discriminator: Discriminator[InitiationParameter, `Unrecognized Parameter`, Int] = Discriminator(8)

    /**
     * Unrecognized Parameter:
     *
     * Parameter Type Value: 8
     *
     * Parameter Length: Variable size.
     *
     * Parameter Value:
     *
     * This parameter is returned to the originator of the INIT chunk
     * when the INIT contains an unrecognized parameter that has a value
     * that indicates it should be reported to the sender.  This
     * parameter value field will contain unrecognized parameters copied
     * from the INIT chunk complete with Parameter Type, Length, and
     * Value fields.
     */
    implicit val codec: Codec[`Unrecognized Parameter`] = {
      variableSizeBytes("Parameter Length" | uint16,
        "Parameter Value" | vector[InitiationParameter](InitiationParameter.codec),
        4)
    }.as[`Unrecognized Parameter`]

  }

  object InitiationParameter {
    implicit val discriminated: Discriminated[InitiationParameter, Int] = Discriminated("Type" | uint16)
    implicit val codec: Codec[InitiationParameter] = Codec.coproduct[InitiationParameter].auto
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
  optionalParameters: Vector[Initiation.InitiationParameter]) extends SctpChunk

/**
 *
 *  Note 1: The INIT ACK chunks can contain any number of IP address
 * parameters that can be IPv4 and/or IPv6 in any combination.
 *
 * Note 2: The ECN Capable field is reserved for future use of Explicit
 * Congestion Notification.
 *
 * Note 3: The INIT ACK chunks MUST NOT contain more than one Host Name
 * Address parameter.  Moreover, the sender of the INIT ACK MUST NOT
 * combine any other address types with the Host Name Address in the
 * INIT ACK.  The receiver of the INIT ACK MUST ignore any other address
 * types if the Host Name Address parameter is present.
 *
 * IMPLEMENTATION NOTE: An implementation MUST be prepared to receive an
 * INIT ACK that is quite large (more than 1500 bytes) due to the
 * variable size of the State Cookie AND the variable address list.  For
 * example if a responder to the INIT has 1000 IPv4 addresses it wishes
 * to send, it would need at least 8,000 bytes to encode this in the
 * INIT ACK.
 *
 * IMPLEMENTATION NOTE: If an INIT ACK chunk is received with known
 * parameters that are not optional parameters of the INIT ACK chunk,
 * then the receiver SHOULD process the INIT ACK chunk and send back a
 * COOKIE ECHO.  The receiver of the INIT ACK chunk MAY bundle an ERROR
 * chunk with the COOKIE ECHO chunk.  However, restrictive
 * implementations MAY send back an ABORT chunk in response to the INIT
 * ACK chunk.
 *
 *
 *
 * @param initiateTag
 *    The receiver of the INIT ACK records the value of the Initiate Tag
 * parameter.  This value MUST be placed into the Verification Tag
 * field of every SCTP packet that the INIT ACK receiver transmits
 * within this association.
 *
 * The Initiate Tag MUST NOT take the value 0.  See Section 5.3.1 for
 * more on the selection of the Initiate Tag value.
 *
 * If the value of the Initiate Tag in a received INIT ACK chunk is
 * found to be 0, the receiver MUST destroy the association
 * discarding its TCB.  The receiver MAY send an ABORT for debugging
 * purpose.
 * @param advertisedReceiverWindowCredit
 *    This value represents the dedicated buffer space, in number of
 * bytes, the sender of the INIT ACK has reserved in association with
 * this window.  During the life of the association, this buffer
 * space SHOULD NOT be lessened (i.e., dedicated buffers taken away
 * from this association).
 * @param numberOfOutboundStreams (OS)
 *                                Defines the number of outbound streams the sender of this INIT ACK
 * chunk wishes to create in this association.  The value of 0 MUST
 * NOT be used, and the value MUST NOT be greater than the MIS value
 * sent in the INIT chunk.
 *
 * Note: A receiver of an INIT ACK with the OS value set to 0 SHOULD
 * destroy the association discarding its TCB.
 * @param numberOfInboundStreams (MIS)
 *    Defines the maximum number of streams the sender of this INIT ACK
 * chunk allows the peer end to create in this association.  The
 * value 0 MUST NOT be used.
 *
 * Note: There is no negotiation of the actual number of streams but
 * instead the two endpoints will use the min(requested, offered).
 * See Section 5.1.1 for details.
 *
 * Note: A receiver of an INIT ACK with the MIS value set to 0 SHOULD
 * destroy the association discarding its TCB.
 * @param initialTsn (I-TSN)
 * Defines the initial TSN that the INIT ACK sender will use.  The
 * valid range is from 0 to 4294967295.  This field MAY be set to the
 * value of the Initiate Tag field.
 *
 */
private[sctp] final case class InitAck(
    initiateTag: Long,
    advertisedReceiverWindowCredit: Long,
    numberOfOutboundStreams: Int,
    numberOfInboundStreams: Int,
    initialTsn: Long,
    optionalParameters: Vector[Initiation.InitiationParameter]) extends SctpChunk {

  require(optionalParameters.count(_.isInstanceOf[Initiation.`Host Name Address`]) <= 1, "The INIT ACK chunks MUST NOT contain more than one Host Name Address parameter.")
  require(optionalParameters.count(_.isInstanceOf[Initiation.`State Cookie`]) == 1, "InitAck must have exactly one State Cookie as optional Parameter.")

}

private[sctp] object InitAck {

  /**
   * The INIT ACK chunk is used to acknowledge the initiation of an SCTP
   * association.
   *
   * The parameter part of INIT ACK is formatted similarly to the INIT
   * chunk.  It uses two extra variable parameters: The State Cookie and
   * the Unrecognized Parameter:
   *
   * The format of the INIT ACK chunk is shown below:
   * {{{
   *
   *
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Type = 2    |  Chunk Flags  |      Chunk Length             |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                         Initiate Tag                          |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |              Advertised Receiver Window Credit                |
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
  implicit val codec: Codec[InitAck] = {
    constant(ChunkType.codec.encode(ChunkType.`INIT ACK`).require) :~>:
      ignore(8) :~>:
      variableSizeBytes("Chunk Length" | uint16,
        ("Initiate Tag" | nonZero(uint32)) ::
          ("Advertised Receiver Window Credit" | uint32) ::
          ("Number of Outbound Streams" | nonZero(uint16)) ::
          ("Number of Inbound Streams" | nonZero(uint16)) ::
          ("Initial TSN" | uint32) ::
          ("Optional Parameters" | vector(InitiationParameter.codec)), 2)
  }.as[InitAck]

}

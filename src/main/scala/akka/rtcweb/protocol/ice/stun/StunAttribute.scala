package akka.rtcweb.protocol.ice.stun

import java.net.{ InetAddress, InetSocketAddress }

import akka.rtcweb.protocol.scodec.SCodecContrib
import akka.rtcweb.protocol.scodec.SCodecContrib._
import scodec._
import scodec.bits.{ BitVector, ByteOrdering, ByteVector, HexStringSyntax }
import scodec.codecs._
import shapeless._

sealed trait StunAttributeType

sealed trait Family

object Family {
  implicit val codec: Codec[Family] = mappedEnum(uint8, IPv4 -> 1, IPv6 -> 2)

  case object IPv4 extends Family

  case object IPv6 extends Family

}

/**
 * Known attribute types from [[https://tools.ietf.org/html/rfc5389#section-18.2]]
 */
object StunAttributeType {
  implicit val codec: Codec[StunAttributeType] = choice({
    mappedEnum(codecs.bits(16),
      `MAPPED-ADDRESS` -> hex"0x0001".bits,
      USERNAME -> hex"0x0006".bits,
      `MESSAGE-INTEGRITY` -> hex"0x0008".bits,
      `ERROR-CODE` -> hex"0x0009".bits,
      `UNKNOWN-ATTRIBUTES` -> hex"0x000A".bits,
      REALM -> hex"0x0014".bits,
      NONCE -> hex"0x0015".bits,
      `XOR-MAPPED-ADDRESS` -> hex"0x0020".bits,
      `ALTERNATE-SERVER` -> hex"0x8023".bits,
      PRIORITY -> hex"0x0024".bits,
      `USE-CANDIDATE` -> hex"0x0025".bits,
      SOFTWARE -> hex"0x8022".bits,
      FINGERPRINT -> hex"0x8028".bits,
      `ICE-CONTROLLED` -> hex"0x8029".bits,
      `ICE-CONTROLLING` -> hex"0x802A".bits
    )
  }, codecs.bits(16).as[UNKNOWN].widenOpt[StunAttributeType](identity, {
    case a: UNKNOWN => Some(a)
    case _ => None
  }))

  final case class UNKNOWN(code: BitVector) extends StunAttributeType {
    require(code.length == 16, "code must be 4 bytes (16 bits) long")

    /**
     * Tells whether a stun implementation is required to understand this StunAttribute
     * @see [[https://tools.ietf.org/html/rfc5389#section-15]]
     */
    def mustUnderstand: Boolean = code.toLong(signed = false, ByteOrdering.LittleEndian) <= hex"0x8000".toLong(signed = false, ByteOrdering.LittleEndian)
  }

  case object `MAPPED-ADDRESS` extends StunAttributeType
  case object USERNAME extends StunAttributeType
  case object `MESSAGE-INTEGRITY` extends StunAttributeType
  case object `ERROR-CODE` extends StunAttributeType
  case object `UNKNOWN-ATTRIBUTES` extends StunAttributeType
  case object REALM extends StunAttributeType
  case object NONCE extends StunAttributeType
  case object `XOR-MAPPED-ADDRESS` extends StunAttributeType
  case object SOFTWARE extends StunAttributeType
  case object `ALTERNATE-SERVER` extends StunAttributeType
  case object PRIORITY extends StunAttributeType
  case object `USE-CANDIDATE` extends StunAttributeType
  case object FINGERPRINT extends StunAttributeType
  case object `ICE-CONTROLLED` extends StunAttributeType
  case object `ICE-CONTROLLING` extends StunAttributeType

}

final case class SOFTWARE(description: String) extends StunAttribute {
  require(description.length < 128, "description must be less than 128 characters")
}

object SOFTWARE {

  implicit val codec: Codec[SOFTWARE] =
    StunAttribute.withAttributeHeader(constantValue(StunAttributeType.SOFTWARE), utf8).as[SOFTWARE]

  implicit val discriminator: Discriminator[StunAttribute, SOFTWARE, StunAttributeType] = Discriminator(StunAttributeType.SOFTWARE)
}

final case class `UNKNOWN-ATTRIBUTES`(unknownAttributeTypes: Vector[StunAttributeType]) extends StunAttribute

object `UNKNOWN-ATTRIBUTES` {
  /**
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |      Attribute 1 Type           |     Attribute 2 Type        |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |      Attribute 3 Type           |     Attribute 4 Type    ...
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   */
  implicit val codec: Codec[`UNKNOWN-ATTRIBUTES`] =
    StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`UNKNOWN-ATTRIBUTES`),
      vector(StunAttributeType.codec)
    ).as[`UNKNOWN-ATTRIBUTES`]
  implicit val discriminator: Discriminator[StunAttribute, `UNKNOWN-ATTRIBUTES`, StunAttributeType] = Discriminator(StunAttributeType.`UNKNOWN-ATTRIBUTES`)
}

/**
 * The ERROR-CODE attribute is used in error response messages.  It
 * contains a numeric error code value in the range of 300 to 699 plus a
 * textual reason phrase encoded in UTF-8 [RFC3629], and is consistent
 * in its code assignments and semantics with SIP [RFC3261] and HTTP
 * [RFC2616].  The reason phrase is meant for user consumption, and can
 * be anything appropriate for the error code.  Recommended reason
 * phrases for the defined error codes are included in the IANA registry
 * for error codes.  The reason phrase MUST be a UTF-8 [RFC3629] encoded
 * sequence of less than 128 characters (which can be as long as 763
 * bytes).
 *
 * @see [[https://tools.ietf.org/html/rfc5389#section-15.6]]
 */
final case class `ERROR-CODE`(errorCode: `ERROR-CODE`.Code, reasonPhrase: String) extends StunAttribute {
  require(reasonPhrase.length < 128, "reasonPhrase must be less than 128 characters")
}

object `ERROR-CODE` {

  sealed trait Code
  object Code {

    implicit val codec: Codec[`ERROR-CODE`.Code] = mappedEnum(classNumberCodec,
      `Try Alternate` -> 300,
      `Bad Request` -> 400,
      Unauthorized -> 401,
      `Unknown Attribute` -> 420,
      `Stale Nonce` -> 438,
      `Role Conflict` -> 487,
      `Server Error` -> 500
    )

    /**
     * Try Alternate: The client should contact an alternate server for
     * this request.  This error response MUST only be sent if the
     * request included a USERNAME attribute and a valid MESSAGE-
     * INTEGRITY attribute; otherwise, it MUST NOT be sent and error
     * code 400 (Bad Request) is suggested.  This error response MUST
     * be protected with the MESSAGE-INTEGRITY attribute, and receivers
     * MUST validate the MESSAGE-INTEGRITY of this response before
     * redirecting themselves to an alternate server.
     */
    case object `Try Alternate` extends Code

    /**
     * Bad Request: The request was malformed.  The client SHOULD NOT
     * retry the request without modification from the previous
     * attempt.  The server may not be able to generate a valid
     * MESSAGE-INTEGRITY for this error, so the client MUST NOT expect
     * a valid MESSAGE-INTEGRITY attribute on this response.
     */
    case object `Bad Request` extends Code

    /**
     * Unauthorized: The request did not contain the correct
     * credentials to proceed.  The client should retry the request
     * with proper credentials.
     */
    case object Unauthorized extends Code

    /**
     * Unknown Attribute: The server received a STUN packet containing
     * a comprehension-required attribute that it did not understand.
     * The server MUST put this unknown attribute in the UNKNOWN-
     * ATTRIBUTE attribute of its error response.
     */
    case object `Unknown Attribute` extends Code

    /**
     * The NONCE used by the client was no longer valid.
     * The client should retry, using the NONCE provided in the
     * response.
     */
    case object `Stale Nonce` extends Code
    /**
     * Server Error: The server has suffered a temporary error.  The
     * client should try again.
     */
    case object `Server Error` extends Code

    /**
     * The Binding request contained either the ICE-
     * CONTROLLING or ICE-CONTROLLED attribute, indicating a role that
     * conflicted with the server.  The server ran a tie-breaker based on
     * the tie-breaker value in the request and determined that the
     * client needs to switch roles.
     * @see [[https://tools.ietf.org/html/rfc5245#section-19.2]]
     */
    case object `Role Conflict` extends Code
  }

  /**
   * The Class represents
   * the hundreds digit of the error code.  The value MUST be between 3
   * and 6.  The Number represents the error code modulo 100, and its
   * value MUST be between 0 and 99.
   */
  private val classNumberCodec: Codec[Int] = {
    {
      "Class" | uint(3)
    } :: {
      "Number" | uint8
    }
  }.xmap[Int]({ case clazz :: number :: HNil => clazz * 100 + number }, { code => code / 100 :: code % 100 :: HNil })

  /**
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |           Reserved, should be 0         |Class|     Number    |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |      Reason Phrase (variable)                                ..
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec = {
    StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`ERROR-CODE`),
      ignore(21) ::
        Code.codec :: {
          "ReasonPhrase" | utf8
        })
  }.as[`ERROR-CODE`]
  implicit val discriminator: Discriminator[StunAttribute, `ERROR-CODE`, StunAttributeType] = Discriminator(StunAttributeType.`ERROR-CODE`)

}

/**
 * The alternate server represents an alternate transport address
 * identifying a different STUN server that the STUN client should try.
 *
 * It is encoded in the same way as [[`MAPPED-ADDRESS`]], and thus refers to a
 * single server by IP address.  The IP address family MUST be identical
 * to that of the source IP address of the request.
 * @see [[https://tools.ietf.org/html/rfc5389#section-15.11]]
 */
final case class `ALTERNATE-SERVER`(family: Family, port: Int, address: InetAddress) extends StunAttribute {
  lazy val inetSocketAddress = new InetSocketAddress(address, port)
}

object `ALTERNATE-SERVER` {
  implicit val codec: Codec[`ALTERNATE-SERVER`] =
    StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`ALTERNATE-SERVER`),
      StunAttribute.addressCodec).as[`ALTERNATE-SERVER`]
  implicit val discriminator: Discriminator[StunAttribute, `ALTERNATE-SERVER`, StunAttributeType] = Discriminator(StunAttributeType.`ALTERNATE-SERVER`)
}

/**
 * The MAPPED-ADDRESS attribute indicates a reflexive transport address
 * of the client.  It consists of an 8-bit address family and a 16-bit
 * port, followed by a fixed-length value representing the IP address.
 * If the address family is IPv4, the address MUST be 32 bits.  If the
 * address family is IPv6, the address MUST be 128 bits.  All fields
 * must be in network byte order.
 * @see [[https://tools.ietf.org/html/rfc5389#section-15.1]]
 */
final case class `MAPPED-ADDRESS`(family: Family, port: Int, address: InetAddress) extends StunAttribute {
  lazy val inetSocketAddress = new InetSocketAddress(address, port)
}

object `MAPPED-ADDRESS` {
  implicit val codec: Codec[`MAPPED-ADDRESS`] =
    StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`MAPPED-ADDRESS`),
      StunAttribute.addressCodec).as[`MAPPED-ADDRESS`]

  implicit val discriminator: Discriminator[StunAttribute, `MAPPED-ADDRESS`, StunAttributeType] = Discriminator(StunAttributeType.`MAPPED-ADDRESS`)
}

final case class `XOR-MAPPED-ADDRESS`(family: Family, port: Int, address: InetAddress) extends StunAttribute {
  lazy val inetSocketAddress = new InetSocketAddress(address, port)
}

object `XOR-MAPPED-ADDRESS` {

  /**
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |x x x x x x x x|    Family     |         X-Port                |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                X-Address (Variable)
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec: Codec[`XOR-MAPPED-ADDRESS`] = {

    StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`XOR-MAPPED-ADDRESS`),
      ignore(8) :: {
        "Family" | Family.codec >>:~ { family =>
          {
            "X-Port" | xPortCodec
          } :: {
            "X-Address" | {
              family match {
                case Family.IPv4 => SCodecContrib.xor(ipv4Address, StunMessage.MAGIC_COOKIE)
                case Family.IPv6 => ??? // cannot be done yet since we do not have the transaction id here //ipv6Address
              }
            }
          }
        }
      }
    )

  }.as[`XOR-MAPPED-ADDRESS`]

  implicit val discriminator: Discriminator[StunAttribute, `XOR-MAPPED-ADDRESS`, StunAttributeType] = Discriminator(StunAttributeType.`XOR-MAPPED-ADDRESS`)
  private final val MAGIC_COOKIE_MSBS = StunMessage.MAGIC_COOKIE.take(16)

  final def xPortCodec: Codec[Int] = new Codec[Int] {
    override def encode(b: Int): Attempt[BitVector] = uint16.encode(b).map(_.xor(StunMessage.MAGIC_COOKIE))

    override def decode(buffer: BitVector): Attempt[DecodeResult[Int]] = {
      val (xPortRaw, rest) = buffer.splitAt(16)
      uint16.decode(xPortRaw.xor(MAGIC_COOKIE_MSBS)).map { case DecodeResult(a, _) => DecodeResult[Int](a, rest) }
    }

    override def sizeBound: SizeBound = SizeBound.exact(16)
  }

}

/** @see [[https://tools.ietf.org/html/rfc5245#section-19.1]]*/
final case class `USE-CANDIDATE`() extends StunAttribute

object `USE-CANDIDATE` {
  implicit val codec: Codec[`USE-CANDIDATE`] = StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`USE-CANDIDATE`),
    ignore(0)
  ).hlist.as[`USE-CANDIDATE`]
  implicit val discriminator: Discriminator[StunAttribute, `USE-CANDIDATE`, StunAttributeType] = Discriminator(StunAttributeType.`USE-CANDIDATE`)
}

/** @see [[https://tools.ietf.org/html/rfc5245#section-19.1]]*/
final case class PRIORITY(byteVector: Long) extends StunAttribute

object PRIORITY {
  implicit val codec: Codec[PRIORITY] = StunAttribute.withAttributeHeader(constantValue(StunAttributeType.PRIORITY),
    uint32
  ).hlist.as[PRIORITY]
  implicit val discriminator: Discriminator[StunAttribute, PRIORITY, StunAttributeType] = Discriminator(StunAttributeType.PRIORITY)
}

/** @see [[https://tools.ietf.org/html/rfc5245#section-19.1]]*/
final case class `ICE-CONTROLLED`(byteVector: ByteVector) extends StunAttribute

object `ICE-CONTROLLED` {
  implicit val codec: Codec[`ICE-CONTROLLED`] = StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`ICE-CONTROLLED`),
    bytes(8)
  ).hlist.as[`ICE-CONTROLLED`]
  implicit val discriminator: Discriminator[StunAttribute, `USE-CANDIDATE`, StunAttributeType] = Discriminator(StunAttributeType.`USE-CANDIDATE`)
}

/** @see [[https://tools.ietf.org/html/rfc5245#section-19.1]]*/
final case class `ICE-CONTROLLING`(byteVector: ByteVector) extends StunAttribute

object `ICE-CONTROLLING` {
  implicit val codec: Codec[`ICE-CONTROLLING`] = StunAttribute.withAttributeHeader(constantValue(StunAttributeType.`ICE-CONTROLLING`),
    bytes(8)
  ).hlist.as[`ICE-CONTROLLING`]
  implicit val discriminator: Discriminator[StunAttribute, `USE-CANDIDATE`, StunAttributeType] = Discriminator(StunAttributeType.`ICE-CONTROLLING`)
}

/**
 * @see [[https://tools.ietf.org/html/rfc5389#section-15]]
 */
sealed trait StunAttribute

object StunAttribute {

  /**
   * {{{
   *  0                   1                   2                   3
   *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |         Type                  |            Length             |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                         Value (variable)                ....
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   *
   * The value in the length field MUST contain the length of the Value
   * part of the attribute, prior to padding, measured in bytes.  Since
   * STUN aligns attributes on 32-bit boundaries, attributes whose content
   * is not a multiple of 4 bytes are padded with 1, 2, or 3 bytes of
   * padding so that its value contains a multiple of 4 bytes.  The
   * padding bits are ignored, and may be any value.
   *
   * Any attribute type MAY appear more than once in a STUN message.
   * Unless specified otherwise, the order of appearance is significant:
   * only the first occurrence needs to be processed by a receiver, and
   * any duplicates MAY be ignored by a receiver.
   *
   * To allow future revisions of this specification to add new attributes
   * if needed, the attribute space is divided into two ranges.
   * Attributes with type values between 0x0000 and 0x7FFF are
   * comprehension-required attributes, which means that the STUN agent
   * cannot successfully process the message unless it understands the
   * attribute.  Attributes with type values between 0x8000 and 0xFFFF are
   * comprehension-optional attributes, which means that those attributes
   * can be ignored by the STUN agent if it does not understand them.
   *
   * The set of STUN attribute types is maintained by IANA.  The initial
   * set defined by this specification is found in Section 18.2.
   *
   * The rest of this section describes the format of the various
   * attributes defined in this specification.
   */
  implicit lazy val codec: Codec[StunAttribute] = Codec.coproduct[StunAttribute].choice.as[StunAttribute]
  implicit val discriminated: Discriminated[StunAttribute, StunAttributeType] = Discriminated(StunAttributeType.codec)

  /** Helper for concrete [[StunAttribute]]s **/
  def withAttributeHeader[A, D](discriminator: Codec[Unit], valueCodec: Codec[A]): Codec[A] = {
    blockalignBytes(
      discriminator ~>
        variableSizeBytes[A]({
          "Length" | uint16
        },
          valueCodec
        ), 4)
  }

  /**
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |0 0 0 0 0 0 0 0|    Family     |           Port                |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                                                               |
   * |                 Address (32 bits or 128 bits)                 |
   * |                                                               |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  def addressCodec: Codec[Family :: Int :: InetAddress :: HNil] = {
    ignore(8) :: {
      "Family" | Family.codec >>:~ { family =>
        {
          "Port" | uint8
        } :: {
          "Address" | {
            family match {
              case Family.IPv4 => ipv4Address
              case Family.IPv6 => ipv6Address
            }
          }
        }
      }
    }
  }.dropUnits

}
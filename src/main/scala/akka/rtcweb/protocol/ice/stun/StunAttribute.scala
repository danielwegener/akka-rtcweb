package akka.rtcweb.protocol.ice.stun

import java.net.InetSocketAddress

import scodec._
import scodec.bits._
import codecs._
import scalaz.\/

sealed trait StunAttributeType

/**
 * Known attribute types from [[https://tools.ietf.org/html/rfc5389#section-18.2]]
 */
object StunAttributeType {
  implicit val codec: Codec[StunAttributeType] = {
    mappedEnum(codecs.bits(16),
      `MAPPED-ADDRESS` -> hex"0x0001".toBitVector,
      USERNAME -> hex"0x0006".toBitVector,
      `MESSAGE-INTEGRITY` -> hex"0x0008".toBitVector,
      `ERROR-CODE` -> hex"0x0009".toBitVector,
      `UNKNOWN-ATTRIBUTES` -> hex"0x000A".toBitVector,
      REALM -> hex"0x0014".toBitVector,
      NONCE -> hex"0x0015".toBitVector,
      `XOR-MAPPED-ADDRESS` -> hex"0x0020".toBitVector,
      SOFTWARE -> hex"0x8022".toBitVector,
      `ALTERNATE-SERVER` -> hex"0x8023".toBitVector,
      FINGERPRINT -> hex"0x8028".toBitVector
    )
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

  case object FINGERPRINT extends StunAttributeType

}

sealed trait StunAttribute

object StunAttribute {

  /**
   * {{{
   *     0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |         Type                  |            Length             |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                         Value (variable)                ....
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec: Codec[StunAttribute] = {
    /*StunAttributeType.codec >>:~ { t =>
      variableSizeBytes({
        "Length" | uint16
      }, lazily {
        t match {
          case StunAttributeType.`MAPPED-ADDRESS` => ??? //`MAPPED-ADDRESS`.codec
        }

      })
    }

  }.as[StunAttribute]*/
    ??? }

}

/**
 * The MAPPED-ADDRESS attribute indicates a reflexive transport address
 * of the client.  It consists of an 8-bit address family and a 16-bit
 * port, followed by a fixed-length value representing the IP address.
 * If the address family is IPv4, the address MUST be 32 bits.  If the
 * address family is IPv6, the address MUST be 128 bits.  All fields
 * must be in network byte order.
 */
case class `MAPPED-ADDRESS`(family: `MAPPED-ADDRESS`.Family, address: InetSocketAddress) extends StunAttribute

object `MAPPED-ADDRESS` {

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
  implicit val codec: Codec[`MAPPED-ADDRESS`] = ??? /*{
    ignore(8) ::
      { "Family" | Family.codec } ::
      { "Port" | uint8 } ::
      { "Address" | ??? }

  }.as[`MAPPED-ADDRESS`]*/

  sealed trait Family

  object Family {
    implicit val codec: Codec[Family] = mappedEnum(uint8, IPv4 -> 1, IPv6 -> 2)
    case object IPv4 extends Family
    case object IPv6 extends Family
  }
}

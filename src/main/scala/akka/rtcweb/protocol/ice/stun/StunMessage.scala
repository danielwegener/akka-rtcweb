package akka.rtcweb.protocol.ice.stun

import scodec._
import scodec.bits._
import codecs._
import shapeless._
import akka.rtcweb.protocol.scodec.SCodecContrib._

import scalaz.{ -\/, \/- }

/**
 * STUN messages are encoded in binary using network-oriented format
 * (most significant byte or octet first, also commonly known as big-
 * endian).  The transmission order is described in detail in Appendix B
 * of RFC 791 [RFC0791].  Unless otherwise noted, numeric constants are
 * in decimal (base 10).
 *
 * All STUN messages MUST start with a 20-byte header followed by zero
 * or more Attributes.  The STUN header contains a STUN message type,
 * magic cookie, transaction ID, and message length.
 *
 * {{{
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |0 0|     STUN Message Type     |         Message Length        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Magic Cookie                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * |                     Transaction ID (96 bits)                  |
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * }}}
 */
object StunMessage {

  implicit lazy val codec: Codec[StunMessage] = "STUN header" | {
    { "0 0" | constant(bin"0b00") } ::
      { "STUN Message Type" | stunMessageTypeCodec } :::
      variableSizeBytes({ "Message Length" | uint16 },
        { "Magic Cookie" | constant(hex"0x2112A442") } ::
          { "Transaction ID" | fixedSizeBits(96, bytes) } ::
          { "Attributes" | vector(StunAttribute.codec) }
      )

  }.dropUnits.as[StunMessage]
  /**
   * {{{
   *      0                 1
   * 2  3  4 5 6 7 8 9 0 1 2 3 4 5
   *
   * +--+--+-+-+-+-+-+-+-+-+-+-+-+-+
   * |M |M |M|M|M|C|M|M|M|C|M|M|M|M|
   * |11|10|9|8|7|1|6|5|4|0|3|2|1|0|
   * +--+--+-+-+-+-+-+-+-+-+-+-+-+-+
   *
   * Figure 3: Format of STUN Message Type Field
   *
   * }}}
   *
   * Here the bits in the message type field are shown as most significant
   * (M11) through least significant (M0).  M11 through M0 represent a 12-
   * bit encoding of the method.  C1 and C0 represent a 2-bit encoding of
   * the class.  A class of 0b00 is a request, a class of 0b01 is an
   * indication, a class of 0b10 is a success response, and a class of
   * 0b11 is an error response.  This specification defines a single
   * method, Binding.  The method and class are orthogonal, so that for
   * each method, a request, success response, error response, and
   * indication are possible for that method.  Extensions defining new
   * methods MUST indicate which classes are permitted for that method.
   */
  val stunMessageTypeBitCodec = {
    codecs.bits(14).xmap[BitVector :: BitVector :: HNil](
      v => v.slice(0, 5) ++ v.slice(6, 9) ++ v.slice(10, 14) :: v.slice(5, 6) ++ v.slice(9, 10) :: HNil,
      {
        case m :: c :: HNil => m.slice(0, 5) ++ c.slice(0, 1) ++ m.slice(5, 8) ++ c.slice(1, 2) ++ m.slice(8, 12)
      }
    )
  }

  //TODO: Can you do this nicer?
  val stunMessageTypeCodec = stunMessageTypeBitCodec.exmap[Class :: Method :: HNil](
    {
      case methodBits :: classBits :: HNil => Class.codec.decode(classBits).flatMap {
        case (_, clazz) => Method.codec.decode(methodBits).map { case (_, method) => clazz :: method :: HNil }
      }
    },
    {
      case clazz :: method :: HNil => Class.codec.encode(clazz).flatMap {
        clazzBits => Method.codec.encode(method).map(methodBits => methodBits :: clazzBits :: HNil)
      }
    }

  )

}

/**
 *
 * @param stunMessageType
 * @param length The message length MUST contain the size, in bytes, of the message
 * not including the 20-byte STUN header.  Since all STUN attributes are
 * padded to a multiple of 4 bytes, the last 2 bits of this field are
 * always zero.  This provides another way to distinguish STUN packets
 * from packets of other protocols.
 * @param transactionId The transaction ID is a 96-bit identifier, used to uniquely identify
 * STUN transactions.  For request/response transactions, the
 * transaction ID is chosen by the STUN client for the request and
 * echoed by the server in the response.  For indications, it is chosen
 * by the agent sending the indication.  It primarily serves to
 * correlate requests with responses, though it also plays a small role in
 * helping to prevent certain types of attacks.  The server also uses
 * the transaction ID as a key to identify each transaction uniquely
 * across all clients.  As such, the transaction ID MUST be uniformly
 * and randomly chosen from the interval 0 .. 2**96-1, and SHOULD be
 * cryptographically random.  Resends of the same request reuse the same
 * transaction ID, but the client MUST choose a new transaction ID for
 * new transactions unless the new request is bit-wise identical to the
 * previous request and sent from the same transport address to the same
 * IP address.  Success and error responses MUST carry the same
 * transaction ID as their corresponding request.  When an agent is
 * acting as a STUN server and STUN client on the same port, the
 * transaction IDs in requests sent by the agent have no relationship to
 * the transaction IDs in requests received by the agent.
 */
case class StunMessage(
    messageClass: Class,
    method: Method,
    transactionId: ByteVector,
    attribute: Vector[StunAttribute] = Vector.empty) {
  require(transactionId.length == 12, "The transactionId MUST be 12 byte (96-bit).")
}

sealed trait Class

object Class {
  implicit val codec: Codec[Class] = mappedEnum(codecs.bits(2),
    request -> bin"00",
    indication -> bin"01",
    successResponse -> bin"10",
    errorResponse -> bin"11"
  )
  case object request extends Class
  case object indication extends Class
  case object successResponse extends Class
  case object errorResponse extends Class
}

sealed trait Method
object Method {
  implicit val codec: Codec[Method] = mappedEnum(codecs.bits(12),
    Binding -> bin"0b000000000001"
  )
  case object Binding extends Method

}

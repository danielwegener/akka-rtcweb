package akka.rtcweb.protocol.dtls.record

import akka.rtcweb.protocol.dtls.ProtocolVersion
import akka.util.ByteString

import scodec._
import scodec.bits._
import codecs._

/**
 *
 *  Implementations MUST NOT send zero-length fragments of Handshake,
 *  Alert, or ChangeCipherSpec content types.  Zero-length fragments of
 *  Application data MAY be sent as they are potentially useful as a
 *  traffic analysis countermeasure.
 *
 * @param `type` The higher-level protocol used to process the enclosed fragment.
 * @param version  The version of the protocol being employed.  This document
 * describes DTLS version 1.2, which uses the version { 254, 253 }.
 * The version value of 254.253 is the 1's complement of DTLS version
 * 1.2.  This maximal spacing between TLS and DTLS version numbers
 * ensures that records from the two protocols can be easily
 * distinguished.  It should be noted that future on-the-wire version
 * numbers of DTLS are decreasing in value (while the true version
 * number is increasing in value.)
 * @param opaqueFragment  The application data.  This data is transparent and treated as an
 *     independent block to be dealt with by the higher-level protocol
 *     specified by the type field.
 *     @param epoch A counter value that is incremented on every cipher state change.
 *     @param sequenceNumber The sequence number for this record.
 *
 */
case class DtlsPlaintext(
  `type`: ContentType,
  version: ProtocolVersion,
  epoch: Int,
  sequenceNumber: Long,
  opaqueFragment: ByteVector)

object DtlsPlaintext {
  implicit val codec = "DTLSPlaintext" | {
    ("type" | ContentType.codec) ::
      ("version" | ProtocolVersion.codec) ::
      ("epoch" | uint16) ::
      ("sequence_number" | ulong(48)) ::
      variableSizeBytes("length" |uint16, "opaque fragment" | bytes)
  }.as[DtlsPlaintext]
}

case class DtlsCompressed(
  `type`: ContentType,
  version: ProtocolVersion,
  epoch: Int,
  sequenceNumber: Long,
  opaqueFragment: ByteVector)

object DtlsCompressed {
  implicit val codec = {
    ("type" | ContentType.codec) ::
      ("version" | ProtocolVersion.codec) ::
      ("epoch" | uint16) ::
      ("sequence_number" | ulong(48)) ::
      variableSizeBytes("length" | uint16, "opaque fragment" | bytes)
  }.as[DtlsCompressed]
}

/**
 *    The encryption and MAC functions translate a TLSCompressed
 *     structure into a TLSCiphertext.  The decryption functions reverse
 *     the process.  The MAC of the record also includes a sequence
 *     number so that missing, extra, or repeated messages are detectable.
 *
 * @param `type` The type field is identical to TLSCompressed.type.
 * @param version The version field is identical to TLSCompressed.version.
 * @param epoch uint16
 * @param sequenceNumber uint48
 * @param fragment  The length (in bytes) of the following TLSCiphertext.fragment.
 * The length MUST NOT exceed  2&#94;14 + 2048.
 */
case class DtlsCiphertext(
  `type`: ContentType,
  version: ProtocolVersion,
  epoch: Int,
  sequenceNumber: Long,
  fragment: ByteVector)

object DtlsCiphertext {
  implicit val codec = "DtlsCiphertext" | {
    ("type" | ContentType.codec) ::
      ("version" | ProtocolVersion.codec) ::
      ("epoch" | uint16) ::
      ("sequence_number" | ulong(48)) ::
      variableSizeBytes("length" |uint16, "fragment" | bytes)
  }.as[DtlsCompressed]
}
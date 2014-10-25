package akka.rtcweb.protocol.dtls.handshake

import scodec._
import scodec.bits._
import codecs._

sealed trait ExtensionType
object ExtensionType {

  case object SignatureAlgorithm extends ExtensionType

  implicit val codec: Codec[ExtensionType] = mappedEnum(uint16, SignatureAlgorithm -> 13)
}

/**
 *  The initial set of extensions is defined in a companion document
 * [TLSEXT].  The list of extension types is maintained by IANA as
 * described in Section 12.
 *
 * An extension type MUST NOT appear in the ServerHello unless the same
 * extension type appeared in the corresponding ClientHello.  If a
 * client receives an extension type in ServerHello that it did not
 * request in the associated ClientHello, it MUST abort the handshake
 * with an unsupported_extension fatal alert.
 * @param extensionData opaque extension_data<0..2&#94;16-1>; variable length
 */
sealed case class Extension(
  extensionType: ExtensionType,
  extensionData: ByteVector)

object Extension {
  implicit val codec = "Extension" | {
    ("extension_type" | ExtensionType.codec) :: variableSizeBytes(uint16, "extensionData" | bytes)
  }.as[Extension]
}
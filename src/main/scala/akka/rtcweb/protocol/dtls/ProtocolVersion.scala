package akka.rtcweb.protocol.dtls

import scodec._
import scodec.bits._
import codecs._

sealed trait ProtocolVersion //(major: UInt8, minor: UInt8)

object ProtocolVersion {

  /** "This Version is not supported by dtls" */
  case object `TLS v1.0` extends ProtocolVersion
  /** "This Version is not supported by dtls" */
  case object `TLS v1.2` extends ProtocolVersion
  case object `DTLS 1.2` extends ProtocolVersion

  implicit val codec: Codec[ProtocolVersion] = "ProtocolVersion" | mappedEnum(bytes(2),
    `TLS v1.0` -> hex"0301",
    `TLS v1.2` -> hex"0303",
    `DTLS 1.2` -> hex"FEFD")

}

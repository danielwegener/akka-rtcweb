package akka.rtcweb.protocol.dtls

import scodec._
import scodec.bits._
import codecs._

sealed trait ProtocolVersion //(major: UInt8, minor: UInt8)

object ProtocolVersion {

  /** "This Version is not supported by dtls" */
  case object `TLS v1.0` extends ProtocolVersion //(0x3.asInstanceOf[UInt8], 0x1.asInstanceOf[UInt8])
  /** "This Version is not supported by dtls" */
  case object `TLS v1.2` extends ProtocolVersion //(0x3.asInstanceOf[UInt8], 0x3.asInstanceOf[UInt8])
  case object `DTLS 1.2` extends ProtocolVersion //(254.asInstanceOf[UInt8], 253.asInstanceOf[UInt8])

  implicit val codec: Codec[ProtocolVersion] = mappedEnum(bytes(2),
    `TLS v1.0` -> hex"0301",
    `TLS v1.2` -> hex"0303",
    `DTLS 1.2` -> hex"FEFD")

}

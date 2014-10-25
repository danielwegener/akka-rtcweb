package akka.rtcweb.protocol.sctp

import scodec._
import scodec.bits._
import codecs._

final case class Port(number: Int) extends AnyVal
case object Port {
  implicit val codec = {
    "number" | uint16
  }.as[Port]
}

case class SctpCommonHeader(
  sourcePort: Port,
  destinationPort: Port,
  verificationTag: Long,
  checksum: Long)

object SctpCommonHeader {
  implicit val codec = {
    ("source_port" | Port.codec) ::
      ("destination_port" | Port.codec) ::
      ("verification_tag" | uint32) ::
      ("checksum" | uint32)
  }.as[SctpCommonHeader]
}
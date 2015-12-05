package akka.rtcweb.protocol.sdp.sctp.renderer

import akka.rtcweb.protocol.sdp.renderer.Rendering
import akka.rtcweb.protocol.sdp.sctp._

trait SctpExtensionAttributeRenderer {

  import akka.rtcweb.protocol.sdp.renderer.Rendering.SP

  def renderSctpExtensionAttribute[R <: Rendering](r: R, a: SctpExtensionAttribute): r.type = a match {
    case SctpPort(portNumber) => r ~ "sctp-port:" ~ portNumber
    case SctpFmtp(associationUsage, maxMessageSize) => r ~ "fmtp:" ~ associationUsage ~ maxMessageSize.map(" " + _).getOrElse("")
    case Sctpmap(number, app, maxMessageSize) => r ~ "sctpmap:" ~ number.toString ~ SP ~ app ~ maxMessageSize.map(" " + _).getOrElse("") // todo: streams
  }

}

package akka.rtcweb.protocol.sdp.sctp.renderer

import akka.rtcweb.protocol.sdp.renderer.Rendering
import akka.rtcweb.protocol.sdp.sctp.{ Sctpmap, Fmtp, SctpPort, SctpExtensionAttribute }

trait SctpExtensionAttributeRenderer {

  import akka.rtcweb.protocol.sdp.renderer.Rendering.SP

  def renderSctpExtensionAttribute[R <: Rendering](r: R, a: SctpExtensionAttribute): r.type = a match {
    case SctpPort(portNumber) => r ~ "sctp-port:" ~ portNumber
    case Fmtp(associationUsage, maxMessageSize) => r ~ associationUsage ~ SP ~ "max-message-size" ~ maxMessageSize
    case Sctpmap(number, app) => r ~ "a=sctpmap:" ~ SP ~ number.toString ~ SP // todo: max-messa-gesize and streams
  }

}

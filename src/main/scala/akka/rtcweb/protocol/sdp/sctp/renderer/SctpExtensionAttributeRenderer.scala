package akka.rtcweb.protocol.sdp.sctp.renderer

import java.net.InetSocketAddress

import akka.rtcweb.protocol.sdp.renderer.{ Renderer, Rendering }
import akka.rtcweb.protocol.sdp.sctp.{ Fmtp, SctpPort, SctpExtensionAttribute }

trait SctpExtensionAttributeRenderer {

  import akka.rtcweb.protocol.sdp.renderer.Rendering.SP

  def renderIceExtensionAttribute[R <: Rendering](r: R, a: SctpExtensionAttribute): r.type = a match {
    case SctpPort(portNumber) => r ~ "sctp-port:" ~ portNumber
    case Fmtp(associationUsage, maxMessageSize) => r ~ associationUsage ~ SP ~ "max-message-size"
  }

}

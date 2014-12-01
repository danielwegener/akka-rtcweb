package akka.rtcweb.protocol.ice.renderer

import java.net.InetSocketAddress

import akka.rtcweb.protocol.ice._
import akka.rtcweb.protocol.sdp.renderer.{ Renderer, Rendering }

trait IceExtensionAttributeRenderer {

  import akka.rtcweb.protocol.sdp.renderer.Rendering.SP

  private implicit val candidateTypeRenderer = new Renderer[CandidateType] {
    override def render[R <: Rendering](r: R, value: CandidateType): r.type = value match {
      case CandidateType.host => r ~ "host"
      case CandidateType.prflx => r ~ "prflx"
      case CandidateType.relay => r ~ "relay"
      case CandidateType.srflx => r ~ "srflx"
      case CandidateType.UnknownCandidateType(token) => r ~ token
    }
  }
  private implicit val inetSocketAddressRender = new Renderer[InetSocketAddress] {
    override def render[R <: Rendering](r: R, addr: InetSocketAddress): r.type = r ~ addr.getHostName ~ SP ~ addr.getPort
  }
  private implicit val priorityRenderer = new Renderer[Priority] {
    override def render[R <: Rendering](r: R, value: Priority): r.type = r ~ value.priority
  }
  private implicit val transportRenderer = new Renderer[Transport] {
    override def render[R <: Rendering](r: R, value: Transport): r.type = value match {
      case Transport.UDP => r ~ "UDP"
      case Transport.UnknownTransportExtension(token) => r ~ token
    }
  }

  def renderIceExtensionAttribute[R <: Rendering](r: R, a: IceExtensionAttribute): r.type = a match {
    case Candidate(foundation, componentId, transport, priority, connectionAddress, candidateType, relayConnectionAddress, extensionAttributes) =>
      r ~ "candidate:" ~ foundation ~ SP ~ componentId ~ SP ~ transport ~ SP ~ priority ~ SP ~ connectionAddress ~ SP ~ "typ" ~ SP ~ candidateType
      relayConnectionAddress.foreach(r ~ SP ~ _)
      extensionAttributes.foreach { case (key, value) => r ~ SP ~ key ~ SP ~ value }
      r
    case RemoteCandidates(candidates) => ???
  }

}

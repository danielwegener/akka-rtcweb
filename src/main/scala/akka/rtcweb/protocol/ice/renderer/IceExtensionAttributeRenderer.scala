package akka.rtcweb.protocol.ice.renderer

import java.net.InetSocketAddress

import akka.rtcweb.protocol.ice.Setup.Role
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

  private implicit val hashFunctionRenderer = new Renderer[HashFunction] {
    override def render[R <: Rendering](r: R, value: HashFunction): r.type = value match {
      case HashFunction.`md2` => r ~ "md2"
      case HashFunction.`md5` => r ~ "md5"
      case HashFunction.`sha-1` => r ~ "sha-1"
      case HashFunction.`sha-224` => r ~ "sha-224"
      case HashFunction.`sha-256` => r ~ "sha-256"
      case HashFunction.`sha-384` => r ~ "sha-384"
      case HashFunction.`sha-512` => r ~ "sha-512"
      case HashFunction.UnknownHashFunction(token) => r ~ token
    }
  }

  private implicit val setupRoleRenderer = new Renderer[Setup.Role] {
    override def render[R <: Rendering](r: R, value: Role): r.type = value match {
      case Setup.Role.active => r ~ "active"
      case Setup.Role.passive => r ~ "passive"
      case Setup.Role.actpass => r ~ "actpass"
      case Setup.Role.holdconn => r ~ "holdconn"
    }
  }

  def renderIceExtensionAttribute[R <: Rendering](r: R, a: IceExtensionAttribute): r.type = a match {
    case Candidate(foundation, componentId, transport, priority, connectionAddress, candidateType, relayConnectionAddress, extensionAttributes) =>
      r ~ "candidate:" ~ foundation ~ SP ~ componentId ~ SP ~ transport ~ SP ~ priority ~ SP ~ connectionAddress ~ SP ~ "typ" ~ SP ~ candidateType
      relayConnectionAddress.foreach(r ~ SP ~ _)
      extensionAttributes.foreach { case (key, value) => r ~ SP ~ key ~ SP ~ value }
      r
    case RemoteCandidates(candidates) => ???
    case IcePwd(password) => r ~ "ice-pwd:" ~ password
    case IceUfrag(ufrag) => r ~ "ice-ufrag:" ~ ufrag
    case Fingerprint(hashFunction, fingerprint) => r ~ "fingerprint:" ~ hashFunction ~ SP ~ fingerprint
    case Setup(role) => r ~ "setup:" ~ role
  }

}

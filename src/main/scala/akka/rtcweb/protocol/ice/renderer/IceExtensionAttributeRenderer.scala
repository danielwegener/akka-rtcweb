package akka.rtcweb.protocol.ice.renderer

import akka.rtcweb.protocol.ice.{ Candidate, IceExtensionAttribute }
import akka.rtcweb.protocol.sdp.renderer.Rendering

trait IceExtensionAttributeRenderer {

  def renderIceExtensionAttribute[R <: Rendering](r: R, a: IceExtensionAttribute): r.type = a match {
    case Candidate(foundation, componentId, transport, priority, connectionAddress, candidateType, relayConnectionAddress, extensionAttributes) =>
      r ~ "candidate:" ~ foundation ~ ???.asInstanceOf[String]

  }

}

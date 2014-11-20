package akka.rtcweb.protocol.sdp.grouping

import akka.rtcweb.protocol.sdp.renderer.{Renderer, Rendering}

trait GroupingExtensionAttributeRenderer {

  import akka.rtcweb.protocol.sdp.renderer.Rendering.SP

  private implicit val semanticsRenderer:Renderer[Semantics] = ???

  def renderGroupingExtensionAttributes[R <: Rendering](r: R, v: GroupingExtensionAttribute): r.type = v match {
    case MediaStreamIdentifier(tag) => r ~ "mid:" ~ tag
    case Group(semantics, streams) => r ~ "group:" ~ semantics; streams.foreach(r ~ SP ~ _.tag); r
  }



}

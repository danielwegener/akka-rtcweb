package akka.rtcweb.protocol.sdp.grouping

import akka.rtcweb.protocol.sdp.renderer.Renderer._
import akka.rtcweb.protocol.sdp.renderer.{ Renderer, Rendering }

trait GroupingExtensionAttributeRenderer {

  import akka.rtcweb.protocol.sdp.renderer.Rendering.SP

  private implicit val semanticsRenderer: Renderer[Semantics] = stringRenderer[Semantics] {
    case Semantics.FID => "FID"
    case Semantics.LS => "LS"
    case Semantics.UnknownSemanticsExtension(name) => name
  }

  def renderGroupingExtensionAttributes[R <: Rendering](r: R, v: GroupingExtensionAttribute): r.type = v match {
    case MediaStreamIdentifier(tag) => r ~ "mid:" ~ tag
    case Group(semantics, streams) => r ~ "group:" ~ semantics; streams.foreach(r ~ SP ~ _.tag); r
  }

}

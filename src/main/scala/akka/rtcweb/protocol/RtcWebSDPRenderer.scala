package akka.rtcweb.protocol

import akka.rtcweb.protocol.ice.IceExtensionAttribute
import akka.rtcweb.protocol.ice.renderer.IceExtensionAttributeRenderer
import akka.rtcweb.protocol.sdp.grouping.{ GroupingExtensionAttribute, GroupingExtensionAttributeRenderer }
import akka.rtcweb.protocol.sdp.sctp.SctpExtensionAttribute
import akka.rtcweb.protocol.sdp.sctp.renderer.SctpExtensionAttributeRenderer
import akka.rtcweb.protocol.sdp.{ SessionDescription, ExtensionAttribute }
import akka.rtcweb.protocol.sdp.renderer.{ ByteStringRendering, StringRendering, Rendering, SessionDescriptionRenderer }
import akka.util.ByteString

class RtcWebSDPRenderer extends SessionDescriptionRenderer with IceExtensionAttributeRenderer with GroupingExtensionAttributeRenderer with SctpExtensionAttributeRenderer {

  override def renderAttributeExtensions[R <: Rendering](r: R, extensionAttribute: ExtensionAttribute): r.type = extensionAttribute match {
    case e: IceExtensionAttribute => renderIceExtensionAttribute(r, e)
    case e: GroupingExtensionAttribute => renderGroupingExtensionAttributes(r, e)
    case e: SctpExtensionAttribute => renderSctpExtensionAttribute(r, e)
  }

  def render(sessionDescription: SessionDescription): String = {
    val renderingContext = new StringRendering
    sessionDescriptionRenderer.render(renderingContext, sessionDescription)
    renderingContext.get
  }

  def renderByteString(sessionDescription: SessionDescription): ByteString = {
    val renderingContext = new ByteStringRendering(1024)
    sessionDescriptionRenderer.render(renderingContext, sessionDescription)
    renderingContext.get
  }

}

package akka.rtcweb.protocol.sdp.grouping

import akka.rtcweb.protocol.sdp.ExtensionAttribute

final case class MediaStreamIdentifier(tag: String) extends ExtensionAttribute {
  override def key: String = "mid"
}


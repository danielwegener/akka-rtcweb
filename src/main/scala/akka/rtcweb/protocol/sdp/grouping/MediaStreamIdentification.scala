package akka.rtcweb.protocol.sdp.grouping

import akka.rtcweb.protocol.sdp.ExtensionAttribute

final case class MediaStreamIdentification(tag:String) extends ExtensionAttribute {
  override def key: String = "mid"
}



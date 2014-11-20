package akka.rtcweb.protocol.ice

import java.net.InetSocketAddress

import akka.rtcweb.protocol.sdp.ExtensionAttribute

/**
 * The attribute contains a connection-address and port for each
   component.  The ordering of components is irrelevant.  However, a
   value MUST be present for each component of a media stream.  This
   attribute MUST be included in an offer by a controlling agent for a
   media stream that is Completed, and MUST NOT be included in any other
   case.
 */
final case class RemoteCandidates(candidates:Map[Int, InetSocketAddress]) extends ExtensionAttribute {
  override def key: String = "remote-candidates"
}



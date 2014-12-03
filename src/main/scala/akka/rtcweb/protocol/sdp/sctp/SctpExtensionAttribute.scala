package akka.rtcweb.protocol.sdp.sctp

import akka.rtcweb.protocol.sdp.ExtensionAttribute

import java.net.InetSocketAddress

sealed trait SctpExtensionAttribute extends ExtensionAttribute

/**
 * This section defines a new SDP media-level attribute, 'sctp-port'.
   The attribute can be associated with an SDP media descriptor (m-
   line) with a 'DTLS/SCTP' proto value, in which case the m- line port
   value indicates the port of the underlying transport protocol (UDP or
   TCP).

   If the SDP sctp-port attribute is not present, the default value is
   5000.

   Usage of the SDP sctp-port attribute with other proto values is not
   specified, and MUST be discarded if received.
 @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-08#section-5]]
 */
final case class SctpPort(port:Int) extends SctpExtensionAttribute {
  override def key: String = "sctp-port"
}

/**
 * The SDP 'fmtp' attribute can be used with an m- line, associated with
   an SCTP association, to indicate the maximum message size that an
   SCTP endpoint is willing to receive, for a particular SCTP
   association usage, on that SCTP association.

   The remote peer MUST assume that larger messages will be rejected by
   the SCTP endpoint.  SCTP endpoints need to decide on appropriate
   behaviour in case a message that exceeds the maximum size needs to be
   sent.

   If the SDP 'fmtp' attribute contains a maximum message size value of
   zero, it indicates the SCTP endpoint will handle messages of any
   size, subject to memory capacity etc.

   If the SDP 'fmtp' attribute is not present, the default value is 64K.
  @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-08#section-6]]
 */
final case class Fmtp(associationUsage:String, maxMessageSize:Long) extends SctpExtensionAttribute {
  override def key: String = "fmtp"
}

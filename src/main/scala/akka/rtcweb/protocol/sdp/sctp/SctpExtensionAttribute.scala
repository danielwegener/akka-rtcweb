package akka.rtcweb.protocol.sdp.sctp

import akka.rtcweb.protocol.sdp.ExtensionAttribute

sealed trait SctpExtensionAttribute extends ExtensionAttribute

/**
 * This section defines a new SDP media-level attribute, 'sctp-port'.
 * The attribute can be associated with an SDP media descriptor (m-
 * line) with a 'DTLS/SCTP' proto value, in which case the m- line port
 * value indicates the port of the underlying transport protocol (UDP or
 * TCP).
 *
 * If the SDP sctp-port attribute is not present, the default value is
 * 5000.
 *
 * Usage of the SDP sctp-port attribute with other proto values is not
 * specified, and MUST be discarded if received.
 * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-08#section-5]]
 */
final case class SctpPort(port: Int) extends SctpExtensionAttribute {
  override def key: String = "sctp-port"
}

/**
 * The SDP 'fmtp' attribute can be used with an m- line, associated with
 * an SCTP association, to indicate the maximum message size that an
 * SCTP endpoint is willing to receive, for a particular SCTP
 * association usage, on that SCTP association.
 *
 * The remote peer MUST assume that larger messages will be rejected by
 * the SCTP endpoint.  SCTP endpoints need to decide on appropriate
 * behaviour in case a message that exceeds the maximum size needs to be
 * sent.
 *
 * If the SDP 'fmtp' attribute contains a maximum message size value of
 * zero, it indicates the SCTP endpoint will handle messages of any
 * size, subject to memory capacity etc.
 *
 * If the SDP 'fmtp' attribute is not present, the default value is 64K.
 * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-08#section-6]]
 */
final case class Fmtp(associationUsage: String, maxMessageSize: Long) extends SctpExtensionAttribute {
  override def key: String = "fmtp"
}

/**
 * The sctpmap attribute maps from a port number (as used in an "m="
 * line) to an encoding name denoting the payload format to be used on
 * top of the SCTP association or the actual protocol running on top of
 * it.
 *
 * The sctpmap MUST include the app parameter indicating the application
 * running on top of the association.
 *
 * The sctpmap line should also contain the max-message-size parameter
 * indicating the maximum message size, in bytes, the endpoint is
 * willing to accept.
 *
 * - The peer should assume that larger message will be rejected by the
 * endpoint, though it is up to the endpoint decide the appropriate
 * behaviour.
 *
 * - A parameter with value of '0' will signal a best effort attempt,
 * subject to the current endpoint memory capacity, to handle
 * messages of any size.
 *
 * - If the parameter is not present, the implementation should provide
 * a default, with a suggested value of 64K.
 *
 * It may also provide the stream parameter to specify the initial
 * number of incoming streams to be supported by each side of the
 * association.
 *
 * - If this parameter is not present, the implementation should
 * provide a default, with a suggested value of 16.
 *
 *
 * For the "a=sctpmap:" attribute line in the offer, there MUST be a
 * corresponding "a=sctpmap:" attribute line in the answer.
 *
 * Any offered association MAY be rejected in the answer, for any
 * reason.  If an association offer is rejected, the offerer and
 * answerer MUST NOT establish an SCTP association for it.  To reject an
 * SCTP association, the SCTP port number in the "a=sctpmap:" attribute
 * line in the answer MUST be set to zero.
 *
 * Any offered association with an "a=sctpmap:" attribute line providing
 * an incoming stream number of zero or larger than 65535 MUST be
 * rejected in the answer.  An offered association answered with an
 * "a=sctpmap:" attribute line providing an incoming stream number of
 * zero or larger than 65535 MUST NOT be established.
 * @see [[https://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-06#section-5.1]]
 */
//@deprecated(since = "draft-ietf-mmusic-sctp-sdp-07")
final case class Sctpmap(number: Long, app: String) extends SctpExtensionAttribute {
  override def key: String = "sctpmap"
}

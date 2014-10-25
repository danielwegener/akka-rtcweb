package akka.rtcweb.protocol.sdp.negotiation

import akka.rtcweb.protocol.sdp.{ PropertyAttribute, ValueAttribute, Attribute, SessionDescription }

object Offer {

  val highestInitialVersionId: Long = 67108863

  /**
   * If the offerer wishes to only receive media
   * from its peer, it MUST mark the stream as recvonly.
   */
  def isRecvOnly(attr: Attribute): Boolean = attr match {
    case PropertyAttribute("recvonly") => true
    case _ => false
  }

  /**
   * If the offerer wishes to only send media on a stream to its peer, it
   * MUST mark the stream as sendonly with the "a=sendonly" attribute.
   */
  def isSendOnly(attr: Attribute): Boolean = attr match {
    case PropertyAttribute("sendonly") => true
    case _ => false
  }

}

/**
 *   The offer (and answer) MUST be a valid SDP message, as defined by RFC
 * 2327 [1], with one exception.  RFC 2327 mandates that either an e or
 * a p line is present in the SDP message.  This specification relaxes
 * that constraint; an SDP formulated for an offer/answer application
 * MAY omit both the e and p lines.  The numeric value of the session id
 * and version in the o line MUST be representable with a 64 bit signed
 * integer.  The initial value of the version MUST be less than
 * (2**62)-1, to avoid rollovers.  Although the SDP specification allows
 * for multiple session descriptions to be concatenated together into a
 * large SDP message, an SDP message used in the offer/answer model MUST
 * contain exactly one session description.
 *
 * The SDP "s=" line conveys the subject of the session, which is
 * reasonably defined for multicast, but ill defined for unicast.  For
 * unicast sessions, it is RECOMMENDED that it consist of a single space
 * character (0x20) or a dash (-).
 *
 * Unfortunately, SDP does not allow the "s=" line to be empty.
 *
 * The SDP "t=" line conveys the time of the session.  Generally,
 * streams for unicast sessions are created and destroyed through
 * external signaling means, such as SIP.  In that case, the "t=" line
 * SHOULD have a value of "0 0".
 *
 * The offer will contain zero or more media streams (each media stream
 * is described by an "m=" line and its associated attributes).  Zero
 * media streams implies that the offerer wishes to communicate, but
 * that the streams for the session will be added at a later time
 * through a modified offer.  The streams MAY be for a mix of unicast
 * and multicast; the latter obviously implies a multicast address in
 * the relevant "c=" line(s).
 *
 * Construction of each offered stream depends on whether the stream is
 * multicast or unicast.
 */
case class Offer(sessionDescription: SessionDescription) {
  //require(sessionDescription)
}

case class Response(sessionDescription: SessionDescription) {

}
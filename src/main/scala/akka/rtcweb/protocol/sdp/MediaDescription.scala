package akka.rtcweb.protocol.sdp

import scala.collection.immutable.Seq

/**
 * A session description may contain a number of media descriptions.
 * Each media description starts with an "m=" field and is terminated by
 * either the next "m=" field or by the end of the session description.
 * {{{
 * m=  (media name and transport address)
 * i=* (media title)
 * c=* (connection information -- optional if included at
 * session level)
 * b=* (zero or more bandwidth information lines)
 * k=* (encryption key)
 * a=* (zero or more media attribute lines)
 * }}}
 */
final case class MediaDescription(
  media: Media,
  mediaTitle: Option[String],
  portRange: PortRange,
  protocol: MediaTransportProtocol,
  mediaAttributes: Seq[Attribute],
  fmt: Seq[String],
  connectionInformation: Option[ConnectionData],
  bandwidthInformation: Seq[BandwidthInformation],
  encryptionKey: Option[EncryptionKey])

/**
 * {{{<media>}}} is the media type.  Currently defined media are "audio",
 * "video", "text", "application", and "message", although this list
 * may be extended in the future (see Section 8).
 */
sealed trait Media
object Media {

  case object audio extends Media
  case object video extends Media
  case object text extends Media
  case object application extends Media
  case object message extends Media
}
case class CustomMedia(token: String) extends Media

final case class PortRange(
  port: Int,
  range: Option[Int] = None)

sealed trait MediaTransportProtocol
object MediaTransportProtocol {
  case object udp extends MediaTransportProtocol
  case object `RTP/AVP` extends MediaTransportProtocol

  /** denotes the Secure Real-time Transport Protocol [23] running over UDP. */
  case object `RTP/SAVP` extends MediaTransportProtocol

  /** Real-time Transport Control Protocol (RTCP)-Based Feedback (RTP/SAVPF) rfc5124. */
  case object `RTP/SAVPF` extends MediaTransportProtocol

  /**
   * When a RTP/SAVP stream is transported over DTLS with UDP
   * @see [[https://tools.ietf.org/html/rfc5764#section-8]]
   */
  case object `UDP/TLS/RTP/SAVP` extends MediaTransportProtocol

  /**
   * When a RTP/SAVPF stream is transported over DTLS with UDP
   * @see [[https://tools.ietf.org/html/rfc5764#section-8]]
   */
  case object `UDP/TLS/RTP/SAVPF` extends MediaTransportProtocol

  /**
   * The 'SCTP' proto value describes an SCTP association, as defined in [RFC4960].
   * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-14#section-4.2]]
   */
  case object SCTP extends MediaTransportProtocol

  /**
   * The 'SCTP/DTLS' proto value describes a Datagram Transport Layer
   * Security (DTLS) [RFC6347] connection on top of an SCTP
   * association, as defined in [RFC6083].
   * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-08#section-4.2]]
   */
  case object `SCTP/DTLS` extends MediaTransportProtocol

  /**
   * The 'DTLS/SCTP' proto value describes an SCTP association on top
   * of a DTLS connection, as defined in
   * [I-D.ietf-tsvwg-sctp-dtls-encaps].
   *
   *  NOTE: In the case of 'DTLS/SCTP', the actual transport protocol below
   * DTLS is either UDP or TCP.
   * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-08#section-4.2]]
   */
  //@deprecated(message = "Removed in draft-ietf-mmusic-sctp-sdp-10", since = "draft-ietf-mmusic-sctp-sdp-10")
  case object `DTLS/SCTP` extends MediaTransportProtocol

  /**
   * The 'UDP/DTLS/SCTP' proto value describes an SCTP association on
   * top of a DTLS connection on top of UDP, as defined in Section 7.
   * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-14#section-4.2]]
   */
  case object `UDP/DTLS/SCTP` extends MediaTransportProtocol

  /**
   * The 'TCP/DTLS/SCTP' proto value describes an SCTP association on
   * top of a DTLS connection on top of TCP, as defined in Section 8.
   * @see [[http://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-14#section-4.2]]
   */
  case object `TCP/DTLS/SCTP` extends MediaTransportProtocol

}
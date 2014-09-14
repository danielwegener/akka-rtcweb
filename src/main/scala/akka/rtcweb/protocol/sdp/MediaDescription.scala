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
  connectionInformation: Seq[ConnectionData],
  encryptionKey:Option[EncryptionKey]
                                   )

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
case class CustomMedia(token:String) extends Media

final case class PortRange(
  port: Int,
  range: Option[Int])

sealed trait MediaTransportProtocol
object MediaTransportProtocol {
  case object udp extends MediaTransportProtocol
  case object `RTP/AVP` extends MediaTransportProtocol

  /** denotes the Secure Real-time Transport Protocol [23] running over UDP. */
  case object `RTP/SAVP` extends MediaTransportProtocol
}
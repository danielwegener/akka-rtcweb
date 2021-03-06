package akka.rtcweb.protocol.sdp.grouping

import akka.rtcweb.protocol.sdp.ExtensionAttribute

sealed trait GroupingExtensionAttribute extends ExtensionAttribute

final case class MediaStreamIdentifier(tag: String) extends GroupingExtensionAttribute {
  override def key: String = "mid"
}

sealed trait Semantics
trait SemanticsExtension extends Semantics

object Semantics {

  /** An unknown extension to the SDP Grouping protocol */
  final case class UnknownSemanticsExtension(name: String) extends SemanticsExtension

  /**
   * Lip Synchronization
   * @see [[https://tools.ietf.org/html/rfc5888]]
   */
  case object LS extends Semantics

  /**
   * Flow Identification
   * @see [[https://tools.ietf.org/html/rfc5888]]
   */
  case object FID extends Semantics
}

final case class Group(semantics: Semantics, streams: Seq[MediaStreamIdentifier]) extends GroupingExtensionAttribute {
  override def key: String = "group"
}

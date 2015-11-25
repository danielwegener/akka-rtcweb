package akka.rtcweb.protocol.sdp.grouping.parser

import akka.parboiled2._
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.grouping.{ Group, MediaStreamIdentifier, Semantics }
import akka.rtcweb.protocol.sdp.parser._

/**
 *  This document defines the "group" session-level attribute, which is
 * used for grouping together different media streams.
 *
 * @see [[https://tools.ietf.org/html/rfc5888#section-5]]
 */
trait GroupParser {

  this: Parser with CommonRules ⇒

  import CharacterClasses._

  def groupMediaAttributeExtensions: Rule1[ExtensionAttribute] = rule { `mid-attribute` }

  /**
   * Media Stream Identification Attribute
   * This document defines the "media stream identification" media
   * attribute, which is used for identifying media streams within a
   * session description.
   * {{{
   * mid-attribute      = "a=mid:" identification-tag
   * identification-tag = token
   * ; token is defined in RFC 4566
   * }}}
   */
  private[grouping] def `mid-attribute`: Rule1[MediaStreamIdentifier] = rule {
    atomic("a=mid:") ~ token ~> (t => MediaStreamIdentifier(t))
  }

  def groupSessionAttributeExtensions: Rule1[ExtensionAttribute] = rule { `group-attribute` }

  /**
   * This document defines the "group" session-level attribute, which is
   * used for grouping together different media streams.
   * {{{
   * group-attribute = "a=group:" semantics
   * (SP identification-tag)
   * }}}
   */
  private[grouping] def `group-attribute`: Rule1[Group] = rule {
    atomic("a=group:") ~ semantics ~ oneOrMore(SP ~ `identification-tag` ~>
      (ident => MediaStreamIdentifier(ident))) ~> ((a, b) => Group(a, b))
  }

  /** {{{semantics = "LS" / "FID" / semantics-extension}}}   */
  private def semantics: Rule1[Semantics] = rule {
    (atomic("LS") ~ push(Semantics.LS)) |
      (atomic("FID") ~ push(Semantics.FID)) |
      `semantics-extension` ~> (ext => Semantics.UnknownSemanticsExtension(ext))
  }

  /**
   * {{{
   * semantics-extension = token
   * ; token is defined in RFC 4566
   * }}}
   */
  private[grouping] def `semantics-extension` = rule { token }

  private[grouping] def `identification-tag` = rule { token }

}

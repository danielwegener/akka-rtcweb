package akka.rtcweb.protocol.sdp.grouping

import akka.parboiled2._
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.parser._

trait GroupingParser extends SessionAttributeExtensionRule with MediaAttributeExtensionRule {

  this: Parser with CommonRules ⇒

  import CharacterClasses._

  override def sessionAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { `group-attribute` }

  /**
   * {{{
   * group-attribute = "a=group:" semantics
   * (SP identification-tag)
   * }}}
   */
  def `group-attribute`: Rule1[Group] = rule {
    str("group:") ~ semantics ~ oneOrMore(SP ~ `identification-tag` ~>
      (ident => MediaStreamIdentifier(ident))) ~> ((a, b) => Group(a, b))
  }

  /** {{{semantics = "LS" / "FID" / semantics-extension}}}   */
  def semantics: Rule1[Semantics] = rule {
    (str("LS") ~ push(Semantics.LS)) |
      (str("FID") ~ push(Semantics.FID)) |
      `semantics-extension` ~> (ext => Semantics.UnknownSemanticsExtension(ext))
  }

  /**
   * {{{
   * semantics-extension = token
   * ; token is defined in RFC 4566
   * }}}
   */
  def `semantics-extension` = rule { token }

  def `identification-tag` = rule { token }

  override def mediaAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { `mid-attribute` }

  /**
   * {{{
   * mid-attribute      = "a=mid:" identification-tag
   * identification-tag = token
   * ; token is defined in RFC 4566
   * }}}
   */
  def `mid-attribute`: Rule1[MediaStreamIdentifier] = rule {
    str("mid:") ~ token ~> (t => MediaStreamIdentifier(t))
  }

}

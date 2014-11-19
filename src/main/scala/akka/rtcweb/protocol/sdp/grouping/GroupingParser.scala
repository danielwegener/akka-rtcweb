package akka.rtcweb.protocol.sdp.grouping

import akka.parboiled2._
import akka.rtcweb.protocol.sdp.parser.{CharacterClasses, StringBuilding, Base64Parsing, CommonRules}

trait GroupingParser {

  this: Parser with CommonRules with Base64Parsing with StringBuilding ⇒

  import CharacterClasses._

  /**
   * {{{
   * mid-attribute      = "a=mid:" identification-tag
   * identification-tag = token
   * ; token is defined in RFC 4566
   * }}}
   */
  def `mid-attribute`:Rule1[MediaStreamIdentification] = rule {
    str("mid:") ~ token ~> (t=>MediaStreamIdentification(t))
  }

  /**
   * {{{
   * group-attribute     = "a=group:" semantics
   * (SP identification-tag)
   * semantics           = "LS" / "FID" / semantics-extension

   * }}}
   */
  def `group-attribute`:Rule1[Group] = rule {
    str("group:") ~ semantics ~ oneOrMore(SP ~ `identification-tag` ~>
      (ident=>MediaStreamIdentification(ident))) ~> ((a:Semantics,b:Seq[MediaStreamIdentification]) => Group(a,b))
  }


  def semantics:Rule1[Semantics] = rule {
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
  def `semantics-extension` = rule{ token }

  def `identification-tag` = rule { token }


}

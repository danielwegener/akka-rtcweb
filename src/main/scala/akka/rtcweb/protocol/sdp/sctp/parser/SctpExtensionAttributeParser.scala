package akka.rtcweb.protocol.sdp.sctp.parser

import akka.parboiled2.{ Parser, Rule1 }
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.parser.CommonRules
import akka.rtcweb.protocol.sdp.sctp.{SctpFmtp, Sctpmap, SctpFmtp$, SctpPort}

trait SctpExtensionAttributeParser extends {
  this: Parser with CommonRules =>

  def sctpMediaAttributeExtensions: Rule1[ExtensionAttribute] = rule { `sctp-port-attr` | `sctpmap-attr`  }

  import akka.rtcweb.protocol.sdp.parser.CharacterClasses._

  /** {{{sctp-port-attr  =  "a=sctp-port:" portnumber }}}  */
  private def `sctp-port-attr` = rule { atomic("a=sctp-port:") ~ `port-number` ~> (pn => SctpPort(pn.toInt)) }

  /** {{{port-number     =  port}}} */
  private def `port-number` = rule { port }

  /** port            =  1*DIGIT */
  private def `port` = rule { integer }

  // FIXME: SCTP-FMTP should be parsed but reject non-SCTP FMTP formatted attributes
  /** {{{sctpmap-attr      =  "a=fmtp:" association-usage [max-message-size]}}} */
  //private def `fmtp-attr`:Rule1[SctpFmtp] =  rule { (atomic("a=fmtp:") ~ `association-usage` ~ optional(SP ~`max-message-size`)) ~> ((usage:String, mms:Option[Long]) => SctpFmtp(usage,mms)) }

  /** association-usage = token */
  private def `association-usage`:Rule1[String] = rule { token }

  /**
   * sctpmap-attr        =  "a=sctpmap:" sctpmap-number
   * app [max-message-size] [streams]
   * sctpmap-number      =  1*DIGIT
   * app                 =  token
   * max-message-size    =  "max-message-size" EQUALS 1*DIGIT
   * streams             =  "streams" EQUALS 1*DIGIT"
   *
   */

  /**
   * {{{sctpmap-attr        =  "a=sctpmap:" sctpmap-number
   * app [max-message-size] [streams]}}}
   */
  private def `sctpmap-attr`: Rule1[Sctpmap] = rule { `sctpmap-number` ~ SP ~ app ~ optional(SP ~ `max-message-size`) ~ optional(SP ~ streams) ~> ((number: Long, app: String, mms: Option[Long], str: Option[Long]) => Sctpmap(number, app, mms)) }

  /** sctpmap-number      =  1*DIGIT */
  private def `sctpmap-number` = integer

  /** {{{app = token}}} */
  private def `app` = token

  /** {{{max-message-size    =  "max-message-size" EQUALS 1*DIGIT}}} */
  private def `max-message-size`: Rule1[Long] = rule { atomic("max-message-size=") ~ integer }

  /** {{{streams             =  "streams" EQUALS 1*DIGIT"}}} */
  private def streams: Rule1[Long] = rule { atomic("streams=") ~ integer }

}

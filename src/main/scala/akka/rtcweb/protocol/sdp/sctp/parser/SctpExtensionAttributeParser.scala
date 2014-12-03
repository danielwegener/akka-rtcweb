package akka.rtcweb.protocol.sdp.sctp.parser

import akka.parboiled2.{Parser, Rule1}
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.parser.CommonRules
import akka.rtcweb.protocol.sdp.sctp.{Fmtp, SctpPort}

trait SctpExtensionAttributeParser extends   {
  this: Parser with CommonRules  =>

  def sctpMediaAttributeExtensions: Rule1[ExtensionAttribute] = rule { `sctp-port-attr` | `fmtp-attr` }



  /** {{{sctp-port-attr  =  "a=sctp-port:" portnumber }}}  */
  private def `sctp-port-attr` = rule { str("sctp-port:") ~ `port-number` ~> (pn => SctpPort(pn.toInt)) }

  /** {{{port-number     =  port}}} */
  private def `port-number` = rule { port }

  /** port            =  1*DIGIT */
  private def `port` = rule { integer }

  /** {{{sctpmap-attr      =  "a=fmtp:" association-usage [max-message-size]}}} */
  private def `fmtp-attr`:Rule1[Fmtp] =  rule { "fmtp:" ~ `association-usage` ~ `max-message-size` ~> ((usage:String, mms:Long) => Fmtp(usage,mms)) }

  /** {{{max-message-size  =  "max-message-size" EQUALS 1*DIGIT}}} */
  private def `max-message-size`:Rule1[Long] = rule { str("max-message-size=") ~ integer }

  /** association-usage = token */
  private def `association-usage`:Rule1[String] = rule { token }



}

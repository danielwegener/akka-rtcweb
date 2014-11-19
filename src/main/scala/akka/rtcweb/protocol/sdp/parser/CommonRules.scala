/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.rtcweb.protocol.sdp.parser

import akka.parboiled2._

private[protocol] trait CommonRules { this: Parser with StringBuilding ⇒
  import CharacterClasses._

  def OCTET = rule { ANY }

  def RWS = rule { oneOrMore(optional(CRLF) ~ oneOrMore(WSP)) } // extended with `obs-fold`

  // ******************************************************************************************
  // http://tools.ietf.org/html/rfc7230#section-3.2.3
  // ******************************************************************************************

  def CRLF = rule { CR ~ LF }

  def token: Rule1[String] = rule { clearSB() ~ oneOrMore(`token-char` ~ appendSB()) ~ push(sb.toString) }

  def text = `byte-string`

  def `byte-string`: Rule1[String] = rule { clearSB() ~ oneOrMore(BCHAR ~ appendSB()) ~ push(sb.toString) }

  def `non-ws-string`: Rule1[String] = rule { clearSB() ~ oneOrMore(NWSVCHAR ~ appendSB()) ~ push(sb.toString) }

  def fasttokenWs: Rule1[String] = rule { clearSB() ~ BCHAR ~ appendSB() ~ oneOrMore((BCHAR | SP) ~ appendSB()) ~ BCHAR ~ appendSB() ~ push(sb.toString) }

  def `obs-text` = rule { "\u0080" - "\u00FF" }

  def `numeric-string`: Rule1[String] = rule { clearSB() ~ oneOrMore(DIGIT ~ appendSB()) ~ push(sb.toString) }

  // ******************************************************************************************
  //                                    helpers
  // ******************************************************************************************

  def listSep = rule { ',' ~ OWS }

  def OWS = rule { zeroOrMore(optional(CRLF) ~ oneOrMore(WSP)) } // extended with `obs-fold`

  def digit = rule { DIGIT ~ push(digitInt(lastChar)) }

  def digit2 = rule { DIGIT ~ DIGIT ~ push(digitInt(charAt(-2)) * 10 + digitInt(lastChar)) }

  private def digitInt(c: Char): Int = c - '0'

  def digit4 = rule {
    DIGIT ~ DIGIT ~ DIGIT ~ DIGIT ~ push(digitInt(charAt(-4)) * 1000 + digitInt(charAt(-3)) * 100 + digitInt(charAt(-2)) * 10 + digitInt(lastChar))
  }

  def ws(c: Char) = rule { c ~ OWS }

  def ws(s: String) = rule { s ~ OWS }

  // def number = rule((capture((1 to 18).times(DIGIT)) ~ !DIGIT) ~> (_.toLong)) | (oneOrMore(DIGIT) ~ push(999999999999999999L))

  /** Positive long value that does not start with a 0 (zero) */
  def integer = rule(
    (capture(POSDIGIT ~ (0 to 17).times(DIGIT)) ~ &(!DIGIT) ~> (_.toLong)
      | oneOrMore(DIGIT) ~ push(999999999999999999L)))

  def numberDifferentThanZero: Rule1[Option[Long]] = rule { (ch('0') ~ push(None)) | (number ~> ((n) ⇒ Option[Long](n))) }

  // parses a potentially long series of digits and extracts its Long value capping at 999,999,999,999,999,999 in case of overflows
  // parses a potentially long series of digits and extracts its Long value capping at 999,999,999,999,999,999 in case of overflows
  def number:Rule1[Long] = rule(
    (capture((1 to 18).times(DIGIT)) ~ &(!DIGIT) ~> (_.toLong)
      | oneOrMore(DIGIT) ~ push(999999999999999999L)))

  private def digitLong(c: Char): Long = (c - '0').toLong

}


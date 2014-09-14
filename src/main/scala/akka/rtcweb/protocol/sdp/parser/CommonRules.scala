/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.rtcweb.protocol.sdp.parser


import scala.collection.immutable.Seq
import akka.parboiled2._
import akka.shapeless._

private[parser] trait CommonRules { this: Parser with StringBuilding ⇒
  import CharacterClasses._

  def CRLF = rule { CR ~ LF }

  def OCTET = rule { ANY }

  // ******************************************************************************************
  // http://tools.ietf.org/html/rfc7230#section-3.2.3
  // ******************************************************************************************

  def OWS = rule { zeroOrMore(optional(CRLF) ~ oneOrMore(WSP)) } // extended with `obs-fold`

  def RWS = rule { oneOrMore(optional(CRLF) ~ oneOrMore(WSP)) } // extended with `obs-fold`

  def `byte-string`: Rule1[String] = rule { clearSB() ~ oneOrMore(BCHAR ~ appendSB()) ~ push(sb.toString) }

  def text = `byte-string`

  def `non-ws-string`: Rule1[String] = rule { clearSB() ~ oneOrMore(NWSVCHAR ~ appendSB()) ~ push(sb.toString) }

  def fasttokenWs: Rule1[String] = rule { clearSB() ~ BCHAR ~ appendSB() ~ oneOrMore((BCHAR | SP) ~ appendSB()) ~ BCHAR ~ appendSB() ~ push(sb.toString) }

  def `obs-text` = rule { "\u0080" - "\u00FF" }

  // ******************************************************************************************
  //                                    helpers
  // ******************************************************************************************

  def listSep = rule { ',' ~ OWS }

  def digit = rule { DIGIT ~ push(digitInt(lastChar)) }

  def digit2 = rule { DIGIT ~ DIGIT ~ push(digitInt(charAt(-2)) * 10 + digitInt(lastChar)) }

  def digit4 = rule {
    DIGIT ~ DIGIT ~ DIGIT ~ DIGIT ~ push(digitInt(charAt(-4)) * 1000 + digitInt(charAt(-3)) * 100 + digitInt(charAt(-2)) * 10 + digitInt(lastChar))
  }

  def ws(c: Char) = rule { c ~ OWS }
  def ws(s: String) = rule { s ~ OWS }

  /** Positive long value that does not start with a 0 (zero) */
  def integer = rule(
    (capture(POSDIGIT ~ (0 to 17).times(DIGIT)) ~ &(!DIGIT) ~> (_.toLong)
      | oneOrMore(DIGIT) ~ push(999999999999999999L)))

  // def number = rule((capture((1 to 18).times(DIGIT)) ~ !DIGIT) ~> (_.toLong)) | (oneOrMore(DIGIT) ~ push(999999999999999999L))

  // parses a potentially long series of digits and extracts its Long value capping at 999,999,999,999,999,999 in case of overflows
  // parses a potentially long series of digits and extracts its Long value capping at 999,999,999,999,999,999 in case of overflows
  def number = rule(
    (capture((1 to 18).times(DIGIT)) ~ &(!DIGIT) ~> (_.toLong)
      | oneOrMore(DIGIT) ~ push(999999999999999999L)))

  def numberDifferentThanZero: Rule1[Option[Long]] = rule { (ch('0') ~ push(None)) | (number ~> ((n) ⇒ Option[Long](n))) }

  private def digitInt(c: Char): Int = c - '0'
  private def digitLong(c: Char): Long = (c - '0').toLong

}


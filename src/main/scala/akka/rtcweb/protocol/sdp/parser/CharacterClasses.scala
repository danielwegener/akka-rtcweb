/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.rtcweb.protocol.sdp.parser

import akka.parboiled2.CharPredicate

// efficient encoding of *7-bit* ASCII characters
private[protocol] object CharacterClasses {

  val WSP = CharPredicate(SP, HTAB)
  val WSPCRLF = WSP ++ CR ++ LF
  /** token-char =          %x21 / %x23-27 / %x2A-2B / %x2D-2E / %x30-39 * / %x41-5A / %x5E-7E **/
  val `token-char` = CharPredicate('\u0021', '\u0023' to '\u0027', '\u002A' to '\u002B', '\u002D' to '\u002E', '\u0030' to '\u0039', '\u0041' to '\u005A',
    '\u005E' to '\u007E')
  val NWSVCHAR = VCHAR ++ CharPredicate('\u0080', '\u00FF')
  /** any byte except NUL, CR, or LF */
  val BCHAR = CharPredicate('\u0001' to '\u0009', '\u000B' to '\u000C', '\u000E' to '\u007F')
  /**  %x01-09/%x0B-0C/%x0E-27/%x2A-3B/%x3D/%x3F-FF */
  val `email-safe` = CharPredicate('\u0001' to '\u0009', '\u000B' to '\u000C', '\u000E' to '\u0027', '\u002A' to '\u003B', '\u003F' to '\u00FF')
  // helpers
  val NONWS = CharPredicate('\u0021', '\u0023' to '\u005B', '\u005D' to '\u007E')
  val DIGIT04 = CharPredicate('0' to '4')
  val DIGIT05 = CharPredicate('0' to '5')
  val colonSlashEOI = CharPredicate(':', '/', akka.parboiled2.EOI)

  // http://tools.ietf.org/html/rfc7230#section-1.2 referencing
  // http://tools.ietf.org/html/rfc5234#appendix-B.1
  def ALPHA = CharPredicate.Alpha

  def LOWER_ALPHA = CharPredicate.LowerAlpha

  def UPPER_ALPHA = CharPredicate.UpperAlpha

  def CR = '\r'

  def DIGIT = CharPredicate.Digit

  def POSDIGIT = CharPredicate.Digit19

  def ALPHANUM = CharPredicate.AlphaNum

  def DQUOTE = '"'

  def HEXDIG = CharPredicate.HexDigit

  def HTAB = '\t'

  def LF = '\n'

  def SP = ' '

  def VCHAR = CharPredicate.Visible

  def DIGIT19 = CharPredicate.Digit19

  require(BCHAR.isMaskBased, "BCHAR must be mask based")
  require(`token-char`.isMaskBased, "BCHAR must be mask based")
}

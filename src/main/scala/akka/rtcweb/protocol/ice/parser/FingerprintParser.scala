package akka.rtcweb.protocol.ice.parser

import akka.parboiled2.{ Rule1, Parser }
import akka.rtcweb.protocol.ice.{ Fingerprint, HashFunction }
import akka.rtcweb.protocol.sdp.parser.{ CommonSdpParser, CommonRules, CharacterClasses }

/**
 * @author Daniel Wegener (Holisticon AG)
 */
trait FingerprintParser {

  this: Parser with CommonRules with CommonSdpParser ⇒

  import CharacterClasses._

  /** {{{fingerprint-attribute  =  "fingerprint" ":" hash-func SP fingerprint}}} */
  def `fingerprint-attribute` = rule { str("fingerprint:") ~ `hash-func` ~ SP ~ fingerprint ~> ((hf: HashFunction, fp: String) => Fingerprint(hf, fp)) }

  /**
   * {{{
   *   hash-func  =  "sha-1" / "sha-224" / "sha-256" / "sha-384" / "sha-512" / "md5" / "md2" / token
   * ; Additional hash functions can only come
   * ; from updates to RFC 3279
   * }}}
   */
  private def `hash-func`: Rule1[HashFunction] = rule {
    str("sha-1") ~ push(HashFunction.`sha-1`) |
      str("sha-224") ~ push(HashFunction.`sha-224`) |
      str("sha-256") ~ push(HashFunction.`sha-256`) |
      str("sha-384") ~ push(HashFunction.`sha-384`) |
      str("sha-512") ~ push(HashFunction.`sha-512`) |
      str("md5") ~ push(HashFunction.`md5`) |
      str("md2") ~ push(HashFunction.`md2`) |
      (token ~> (t => HashFunction.UnknownHashFunction(t)))
  }
  //; Additional hash functions can only come
  //; from updates to RFC 3279

  /**
   * {{{
   *   fingerprint            =  2UHEX *(":" 2UHEX)
   * ; Each byte in upper-case hex, separated
   * ; by colons.
   *
   * }}}
   */
  private def fingerprint: Rule1[String] = rule { capture(2.times(HEXDIG) ~ zeroOrMore(":" ~ 2.times(HEXDIG))) }
  //; Each byte in upper-case hex, separated
  //; by colons.

  /**
   * {{{UHEX                   =  DIGIT / %x41-46 ; A-F uppercase}}}
   */
  private val UHEX = UHEXDIG

}

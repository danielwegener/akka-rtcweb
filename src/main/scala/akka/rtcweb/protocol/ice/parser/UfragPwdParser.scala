package akka.rtcweb.protocol.ice.parser

import akka.parboiled2.{ Rule1, Parser }
import akka.rtcweb.protocol.ice.{ IceUfrag, IcePwd }
import akka.rtcweb.protocol.sdp.parser.{ CharacterClasses, CommonSdpParser, CommonRules }

/**
 * @see [[https://tools.ietf.org/html/rfc5245#section-15.4]]
 */
private[ice] trait UfragPwdParser {
  this: Parser with CommonRules with CommonSdpParser with CommonIceParser ⇒

  import CharacterClasses._

  def `ice-pwd-att`: Rule1[IcePwd] = rule { str("ice-pwd:") ~ password ~> (pwd => IcePwd(pwd)) }
  def `ice-ufrag-att`: Rule1[IceUfrag] = rule { str("ice-ufrag:") ~ ufrag ~> (ufrag => IceUfrag(ufrag)) }
  def `password`: Rule1[String] = rule { capture(22 to 256 times `ice-char`) }
  def `ufrag`: Rule1[String] = rule { capture(4 to 256 times `ice-char`) }

}

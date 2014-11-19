package akka.rtcweb.protocol.ice.parser

import java.net.{InetSocketAddress, InetAddress}

import akka.parboiled2._
import akka.rtcweb.protocol.ice.CandidateType.UnknownCandidateType
import akka.rtcweb.protocol.ice.{ CandidateType, Transport }
import akka.rtcweb.protocol.ice.Transport.UnknownTransportExtension
import akka.rtcweb.protocol.sdp.parser.{ CommonSdpParser, CharacterClasses, CommonRules, MediaAttributeExtensionRule }

/**
 * @see [[https://tools.ietf.org/html/rfc5245#section-15.1]]
 */
trait CandidateParser extends MediaAttributeExtensionRule {

  this: Parser with CommonRules with CommonSdpParser ⇒

  import CharacterClasses._

  val `ice-char` = CharPredicate(ALPHA, DIGIT, '+', '/')

  require(`ice-char`.isMaskBased, "BCHAR must be mask based")

  /**
   * {{{candidate-attribute   = "candidate" ":" foundation SP component-id SP
   * transport SP
   * priority SP
   * connection-address SP     ;from RFC 4566
   * port         ;port from RFC 4566
   * SP cand-type
   * [SP rel-addr]
   * [SP rel-port]
   * (SP extension-att-name SP extension-att-value)
   * }}}
   *
   */
  def `candidate-attribute` = rule {
    str("candidate:") ~ foundation ~ SP ~ `component-id` ~
      transport ~ SP ~
      priority ~ SP ~
      priority ~ SP ~
      `connection-address` ~ SP ~
      port ~
      SP ~ `cand-type` ~
      optional(SP ~ `rel-addr`) ~
      optional(SP ~ `rel-port`) ~
      zeroOrMore(SP ~ `extension-att-name` ~ SP ~ `extension-att-value` ~> ((k:String,v:String) => (k,v)))

  }

  /** {{{foundation = 1*32ice-char}}} */
  def foundation:Rule1[String] = rule { capture((1 to 32).times(`ice-char`)) }

  /** {{{component-id = 1*5DIGIT}}} */
  def `component-id`:Rule1[Int] = rule { capture((1 to 5).times(DIGIT))  ~> ((s:String)=>s.toInt) }

  /** {{{transport = "UDP" / transport-extension}}} */
  def transport: Rule1[Transport] = rule { (str("UDP") ~ push(Transport.UDP)) | `transport-extension` }

  /** {{{transport-extension = token ; from RFC 3261}}} */
  def `transport-extension`: Rule1[UnknownTransportExtension] = rule { token ~> (t => UnknownTransportExtension(t)) }

  /** {{{priority = 1*10DIGIT}}} */
  def priority:Rule1[Int] = rule { capture((1 to 10).times(DIGIT)) ~> ((l:String)=>l.toInt) }

  /** {{{cand-type = "typ" SP candidate-types}}} */
  def `cand-type`: Rule1[CandidateType] = rule { str("typ") ~ SP ~ `candidate-types` }

  /** {{{candidate-types = "host" / "srflx" / "prflx" / "relay" / token}}} */
  def `candidate-types`: Rule1[CandidateType] = rule {
    str("host") ~ push(CandidateType.host) |
      str("srflx") ~ push(CandidateType.srflx) |
      str("prflx") ~ push(CandidateType.prflx) |
      str("relay") ~ push(CandidateType.relay) |
      token ~> (t => UnknownCandidateType(t))
  }

  /** {{{rel-addr = "raddr" SP connection-address}}} */
  def `rel-addr`: Rule1[InetSocketAddress] = rule { str("raddr") ~ SP ~ `connection-address` }

  /** {{{rel-port = "rport" SP port}}} */
  def `rel-port`: Rule1[Int] = rule { str("rport") ~ SP ~ port }

  /** {{{extension-att-name = byte-string}}} */
  def `extension-att-name`: Rule1[String] = rule { `byte-string` }

  /** {{{extension-att-name = byte-string}}} */
  def `extension-att-value`: Rule1[String] = rule { `byte-string` }

}

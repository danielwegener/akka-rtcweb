package akka.rtcweb.protocol.ice.parser

import java.net.InetSocketAddress

import akka.parboiled2._
import akka.rtcweb.protocol.ice.CandidateType.UnknownCandidateType
import akka.rtcweb.protocol.ice._
import akka.rtcweb.protocol.ice.Transport.UnknownTransportExtension
import akka.rtcweb.protocol.sdp.parser.{ CommonSdpParser, CharacterClasses, CommonRules }

/**
 * @see [[https://tools.ietf.org/html/rfc5245#section-15.1]]
 */
private[ice] trait CandidateParser {

  this: Parser with CommonRules with CommonIceParser with CommonSdpParser ⇒

  import CharacterClasses._

  /**
   * The candidate attribute is a media-level attribute only.  It contains
   a transport address for a candidate that can be used for connectivity
   checks.

   * {{{candidate-attribute   = "candidate" ":" foundation SP component-id SP
   * transport SP
   * priority SP
   * connection-address SP     ;from RFC 4566
   * port         ;port from RFC 4566
   * SP cand-type
   * [SP rel-addr]
   * [SP rel-port]
   * *(SP extension-att-name SP extension-att-value)
   * }}}
   *
   */
  def `candidate-attribute`: Rule1[Candidate] = rule {
    str("candidate:") ~ foundation ~ SP ~ `component-id` ~
      transport ~ SP ~
      (priority ~> (p => Priority(p))) ~ SP ~
      `connection-address SP port` ~
      SP ~ `cand-type` ~
      optional(SP ~ `rel-addr`) ~
      optional(SP ~ `rel-port`) ~
      zeroOrMore((SP ~ `extension-att-name` ~ SP ~
        `extension-att-value`) ~> ((k:String,v:String) => (k,v))) ~>
      ((f: String, c: Int, t: Transport, pri: Priority, ca: InetSocketAddress, ct: CandidateType, rela: Option[InetSocketAddress], relpo: Option[Int], eatt: Seq[(String,String)]) =>
        Candidate(f, c, t, pri, ca, ct, rela, relpo, eatt)
      )

  }

  /** {{{foundation = 1*32ice-char}}} */
  private def foundation: Rule1[String] = rule { capture((1 to 32).times(`ice-char`)) }

  /** {{{transport = "UDP" / transport-extension}}} */
  private def transport: Rule1[Transport] = rule { (str("UDP") ~ push(Transport.UDP)) | `transport-extension` }

  /** {{{transport-extension = token ; from RFC 3261}}} */
  private def `transport-extension`: Rule1[UnknownTransportExtension] = rule { token ~> (t => UnknownTransportExtension(t)) }

  /** {{{priority = 1*10DIGIT}}} */
  private def priority: Rule1[Long] = rule { capture((1 to 10).times(DIGIT) ~ &(!DIGIT)) ~> ((l: String) => l.toLong) }

  /** {{{cand-type = "typ" SP candidate-types}}} */
  private def `cand-type`: Rule1[CandidateType] = rule { str("typ") ~ SP ~ `candidate-types` }

  /** {{{candidate-types = "host" / "srflx" / "prflx" / "relay" / token}}} */
  private def `candidate-types`: Rule1[CandidateType] = rule {
    str("host") ~ push(CandidateType.host) |
      str("srflx") ~ push(CandidateType.srflx) |
      str("prflx") ~ push(CandidateType.prflx) |
      str("relay") ~ push(CandidateType.relay) |
      token ~> (t => UnknownCandidateType(t))
  }

  /** {{{rel-addr = "raddr" SP connection-address}}} */
  private def `rel-addr`: Rule1[InetSocketAddress] = rule { str("raddr") ~ SP ~ `connection-address` }

  /** {{{rel-port = "rport" SP port}}} */
  private def `rel-port`: Rule1[Int] = rule { str("rport") ~ SP ~ port }

  /** {{{extension-att-name = byte-string}}} */
  private def `extension-att-name`: Rule1[String] = rule { `byte-string` }

  /** {{{extension-att-name = byte-string}}} */
  private def `extension-att-value`: Rule1[String] = rule { `byte-string` }

}

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
   * a transport address for a candidate that can be used for connectivity
   * checks.
   *
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
    str("candidate:") ~ foundation ~ SP ~ `component-id` ~ SP ~
      transport ~ SP ~
      (priority ~ SP ~> (p => Priority(p))) ~
      `connection-address SP port` ~
      SP ~ `cand-type` ~
      optional(SP ~ `rel-addr`) ~
      optional(SP ~ `rel-port`) ~
      zeroOrMore((SP ~ `extension-att-name` ~ SP ~
        `extension-att-value`) ~> ((k: String, v: String) => (k, v))) ~>
      ((f: String, c: Int, t: Transport, pri: Priority, ca: InetSocketAddress, ct: CandidateType, rela: Option[InetSocketAddress], relpo: Option[Int], eatt: Seq[(String, String)]) => {
        val relAddr = for (a <- rela; b <- relpo) yield InetSocketAddress.createUnresolved(a.getHostString, b)
        Candidate(f, c, t, pri, ca, ct, relAddr, eatt)
      }
      )

  }

  /** {{{foundation = 1*32ice-char}}} */
  private def foundation: Rule1[String] = rule { capture((1 to 32).times(`ice-char`)) }

  /** {{{transport = "UDP" / transport-extension}}} */
  /** TODO: It seems to be a bug that some implementations send an udp instead of an UDP */
  private def transport: Rule1[Transport] = rule { ((str("UDP") | str("udp")) ~ push(Transport.UDP)) | `transport-extension` }

  /** {{{transport-extension = token ; from RFC 3261}}} */
  private def `transport-extension`: Rule1[UnknownTransportExtension] = rule { token ~> (t => UnknownTransportExtension(t)) }

  /** {{{priority = 1*10DIGIT}}} */
  private def priority: Rule1[Long] = rule { capture((1 to 10).times(DIGIT) ~ &(!DIGIT)) ~> ((l: String) => l.toLong) }

  /** {{{cand-type = "typ" SP candidate-types}}} */
  private def `cand-type`: Rule1[CandidateType] = rule { str("typ") ~ SP ~ `candidate-types` }

  /** {{{candidate-types = "host" / "srflx" / "prflx" / "relay" / token}}} */
  private def `candidate-types`: Rule1[CandidateType] = rule {
    str("host") ~ push(CandidateType.HostCandidate) |
      str("srflx") ~ push(CandidateType.ServerReflexiveCandidate) |
      str("prflx") ~ push(CandidateType.PeerReflexiveCandidate) |
      str("relay") ~ push(CandidateType.RelayCandidate) |
      token ~> (t => UnknownCandidateType(t))
  }

  /** {{{rel-addr = "raddr" SP connection-address}}} */
  private def `rel-addr`: Rule1[InetSocketAddress] = rule { str("raddr") ~ SP ~ `connection-address` }

  /** {{{rel-port = "rport" SP port}}} */
  private def `rel-port`: Rule1[Int] = rule { str("rport") ~ SP ~ port }

  //FIXME: The spec says its a `byte-string` but a `non-ws-string` makes much more sense here!
  /** {{{extension-att-name = byte-string}}} */
  private def `extension-att-name`: Rule1[String] = rule { `non-ws-string` }

  //FIXME: The spec says its a `byte-string` but a `non-ws-string` makes much more sense here!
  /** {{{extension-att-name = byte-string}}} */
  private def `extension-att-value`: Rule1[String] = rule { `non-ws-string` }

}

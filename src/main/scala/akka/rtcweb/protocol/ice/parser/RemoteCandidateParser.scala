package akka.rtcweb.protocol.ice.parser

import java.net.InetSocketAddress

import akka.parboiled2.{Rule1, Parser}
import akka.rtcweb.protocol.ice.RemoteCandidates
import akka.rtcweb.protocol.sdp.parser.{CharacterClasses, CommonSdpParser, CommonRules}
import scala.collection.immutable.Seq

private[ice] trait RemoteCandidateParser {

  this: Parser with CommonRules with CommonSdpParser with CommonIceParser ⇒

  import CharacterClasses._

  /**
   * The remote-candidates attribute is a media-level attribute only.
   * {{{remote-candidate-att = "remote-candidates" ":" remote-candidate
                           0*(SP remote-candidate)}}}
   */
  def `remote-candidate-att`:Rule1[RemoteCandidates] = rule {
    str("remote-candidates:") ~ `remote-candidate` ~ zeroOrMore(SP ~ `remote-candidate`) ~>
      ((head:(Int, InetSocketAddress), tail:Seq[(Int, InetSocketAddress)]) => RemoteCandidates((head +: tail).toMap))
  }

  /** {{{remote-candidate = component-ID SP connection-address SP port}}} */
  def `remote-candidate`:Rule1[(Int, InetSocketAddress)] = rule {
    `component-id` ~ SP ~ `connection-address SP port` ~> ((ci:Int, addr:InetSocketAddress) => (ci, addr))
  }


}

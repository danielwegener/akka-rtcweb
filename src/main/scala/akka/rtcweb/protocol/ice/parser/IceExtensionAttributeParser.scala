package akka.rtcweb.protocol.ice.parser

import akka.parboiled2.{ Rule1, Parser }
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.parser.{ CommonSdpParser, CommonRules }

trait IceExtensionAttributeParser extends CommonIceParser with CandidateParser with RemoteCandidateParser with UfragPwdParser {
  this: Parser with CommonRules with CommonSdpParser =>

  def iceMediaAttributeExtensions: Rule1[ExtensionAttribute] = rule { `candidate-attribute` | `remote-candidate-att` | `ice-ufrag-att` | `ice-pwd-att` }
  def iceSessionAttributeExtensions: Rule1[ExtensionAttribute] = rule { `ice-ufrag-att` | `ice-pwd-att` }

}

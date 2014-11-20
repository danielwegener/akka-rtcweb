package akka.rtcweb.protocol.ice.parser

import akka.parboiled2.{Rule1, Parser}
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.parser.{CommonSdpParser, CommonRules}

trait IceAttributeExtensionParser extends CommonIceParser with CandidateParser with RemoteCandidateParser {
  this : Parser with CommonRules with CommonSdpParser =>

  def iceMediaAttributeExtensions:Rule1[ExtensionAttribute] = rule { `candidate-attribute` | `remote-candidate-att` }
  def iceSessionAttributeExtensions:Rule1[ExtensionAttribute] = rule { str("NEVER") ~ push(null) }

}

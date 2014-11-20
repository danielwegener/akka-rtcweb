package akka.rtcweb.protocol

import akka.parboiled2._
import akka.rtcweb.protocol.ice.parser.CandidateParser
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.grouping.GroupingParser
import akka.rtcweb.protocol.sdp.parser._
import akka.shapeless._

trait RtcWebSDPParser extends Parser
with CommonSdpParser
with SessionDescriptionParser
with MediaParser
with MediaAttributeExtensionRule
with SessionAttributeExtensionRule
with GroupingParser
with CandidateParser
with CommonRules
with StringBuilding
with Base64Parsing {


  override def sessionAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { `group-attribute` | `candidate-attribute` }

  override def mediaAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { MISMATCH[ExtensionAttribute :: HNil] }
}

package akka.rtcweb.protocol

import akka.parboiled2._
import akka.rtcweb.protocol.ice.parser.{IceAttributeExtensionParser, CandidateParser}
import akka.rtcweb.protocol.sdp.ExtensionAttribute
import akka.rtcweb.protocol.sdp.grouping.GroupParser
import akka.rtcweb.protocol.sdp.parser._
import akka.shapeless._

trait RtcWebSDPParser extends Parser
    with CommonSdpParser
    with SessionDescriptionParser
    with MediaParser
    with MediaAttributeExtensionRule
    with SessionAttributeExtensionRule
    with GroupParser
    with IceAttributeExtensionParser
    with CommonRules
    with StringBuilding
    with Base64Parsing {

    override def sessionAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { groupMediaAttributeExtensions | iceMediaAttributeExtensions }
    override def mediaAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { groupSessionAttributeExtensions | iceSessionAttributeExtensions }

}

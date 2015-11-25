package akka.rtcweb.protocol

import akka.parboiled2._
import akka.rtcweb.protocol.ice.parser.IceExtensionAttributeParser
import akka.rtcweb.protocol.sdp.{SessionDescription, ExtensionAttribute}
import akka.rtcweb.protocol.sdp.grouping.parser.GroupParser
import akka.rtcweb.protocol.sdp.parser._
import akka.rtcweb.protocol.sdp.sctp.parser.SctpExtensionAttributeParser

import scala.util.Try

object RtcWebSDPParser {
    def parserFor(input:ParserInput):RtcWebSDPParser = new RtcWebSDPParserImpl(input)

}

private class RtcWebSDPParserImpl(override val input: ParserInput) extends RtcWebSDPParser {
}

trait RtcWebSDPParser extends Parser
    with CommonSdpParser
    with SessionDescriptionParser
    with MediaParser
    with MediaAttributeExtensionRule
    with SessionAttributeExtensionRule
    with GroupParser
    with IceExtensionAttributeParser
    with SctpExtensionAttributeParser
    with CommonRules
    with StringBuilding
    with Base64Parsing {




  override def sessionAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { groupSessionAttributeExtensions | iceSessionAttributeExtensions }
  override def mediaAttributesExtensionsRule: Rule1[ExtensionAttribute] = rule { groupMediaAttributeExtensions | iceMediaAttributeExtensions | sctpMediaAttributeExtensions }

}

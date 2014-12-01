package akka.rtcweb.protocol.ice.parser

import java.net.InetSocketAddress

import akka.parboiled2.{ ParseError, ParserInput, Parser }
import akka.rtcweb.protocol.ice.{ CandidateType, Priority, Transport, Candidate }
import akka.rtcweb.protocol.sdp.parser.{ Base64Parsing, StringBuilding, CommonSdpParser, CommonRules }
import org.scalatest.{ Inside, Matchers, WordSpecLike }

class CandidateParserSpec extends WordSpecLike with Matchers with Inside {

  "A CandidateParser" should {

    class TestCandidateParser(val input: ParserInput) extends Parser with CandidateParser with CommonRules with CommonIceParser with StringBuilding with CommonSdpParser with Base64Parsing

    "parse the rfc example" in {
      val parser = new TestCandidateParser(ParserInput("candidate:foundation 1 UDP 2 localhost 40678 typ host generation 0"))
      val result = parser.`candidate-attribute`.run().recover { case e @ ParseError(position, traces) => fail(s"\n${parser.formatErrorProblem(e)}: ${parser.formatErrorLine(e)}: ${e.formatTraces}", e) }.get

      result should be(Candidate(
        foundation = "foundation",
        componentId = 1,
        transport = Transport.UDP,
        priority = Priority(2),
        InetSocketAddress.createUnresolved("localhost", 40678),
        candidateType = CandidateType.host,
        relayConnectionAddress = None,
        extensionAttributes = List(("generation", "0")))
      )

    }

    "parse a protocol extension" in {
      val parser = new TestCandidateParser(ParserInput("candidate:1738249477 1 rfid 2122260223 192.168.43.1 40678 typ host generation 0"))
      val result = parser.`candidate-attribute`.run().recover { case e @ ParseError(position, traces) => fail(s"\n${parser.formatErrorProblem(e)}: ${parser.formatErrorLine(e)}: ${e.formatTraces}", e) }.get
      result should be(Candidate(
        foundation = "1738249477",
        componentId = 1,
        transport = Transport.UnknownTransportExtension("rfid"),
        priority = Priority(2122260223L),
        InetSocketAddress.createUnresolved("192.168.43.1", 40678),
        candidateType = CandidateType.host,
        relayConnectionAddress = None,
        extensionAttributes = List(("generation", "0")))
      )
    }
  }

}

package akka.rtcweb.protocol.ice.parser

import java.net.InetSocketAddress

import akka.parboiled2.{ParserInput, Parser}
import akka.rtcweb.protocol.ice.{CandidateType, Priority, Transport, Candidate}
import akka.rtcweb.protocol.sdp.parser.{Base64Parsing, StringBuilding, CommonSdpParser, CommonRules}
import org.scalatest.{Matchers, WordSpecLike}

import scala.util.Try


class CandidateParserSpec extends WordSpecLike with Matchers {

  "A CandidateParser" should {

    class TestCandidateParser(val input:ParserInput) extends Parser with CandidateParser with CommonRules with CommonIceParser with StringBuilding with CommonSdpParser with Base64Parsing

    "parse the rfc example" in {
      val result:Try[Candidate] = new TestCandidateParser(ParserInput("foo")).`candidate-attribute`.run()
      result should be(Candidate(
        "foundation",
        1,
        Transport.UDP,
        Priority(1),
        InetSocketAddress.createUnresolved("localhost",1),
        CandidateType.host, None, None, Nil))

    }
  }

}

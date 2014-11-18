package akka.rtcweb.protocol.sdp.parser

import java.net.InetSocketAddress

import akka.parboiled2.ParseError
import akka.parboiled2.ParserInput.StringBasedParserInput
import akka.rtcweb.protocol.sdp._
import akka.rtcweb.protocol.sdp.renderer.SdpRendering
import scala.collection.immutable.Seq
import org.scalatest.{ Matchers, WordSpecLike }

class SessionDescriptionParserSpec extends WordSpecLike with Matchers {

  "A SessionDescriptorParser" should {

    "parse the rfc example" in {

      val parser = new SessionDescriptionParserImpl(input(
        """v=0
          |o=- 5817373415835868156 2 IN IP4 127.0.0.1
          |s=-
          |t=0 0
          |a=group:BUNDLE audio data
          |a=msid-semantic: WMS
          |m=audio 40678 RTP/SAVPF 111 103 104 0 8 106 105 13 126
          |c=IN IP4 192.168.43.47
          |a=rtcp:40678 IN IP4 192.168.0.1
          |a=candidate:1738249477 1 udp 2122260223 192.168.43.47 40678 typ host generation 0
          |a=candidate:1738249477 2 udp 2122260223 192.168.43.47 40678 typ host generation 0
          |a=candidate:211962667 1 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:211962667 2 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:2441410931 1 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=candidate:2441410931 2 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=ice-ufrag:wAYPGvXic8UghxF8
          |a=ice-pwd:KAo7HuggRkuhYvI3xhT5uVCTc
          |a=ice-options:google-ice
          |a=fingerprint:sha-256 C4:CF:A0:C5:DA:59:2E:79:6D:EF:F3:7F:51:B5:E7:93:95:B0:82:66:3C:8B:34:7B:88:0C:B0:DD:F2:7E:EA:77
          |a=setup:actpass
          |a=mid:audio
          |a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level
          |a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
          |a=recvonly
          |a=rtcp-mux
          |a=rtpmap:111 opus/48000/2
          |a=fmtp:111 minptime=10
          |a=rtpmap:103 ISAC/16000
          |a=rtpmap:104 ISAC/32000
          |a=rtpmap:0 PCMU/8000
          |a=rtpmap:8 PCMA/8000
          |a=rtpmap:106 CN/32000
          |a=rtpmap:105 CN/16000
          |a=rtpmap:13 CN/8000
          |a=rtpmap:126 telephone-event/8000
          |a=maxptime:60
          |m=application 40678 RTP/SAVPF 101
          |c=IN IP4 192.168.43.1
          |a=rtcp:40678 IN IP4 192.168.43.1
          |a=candidate:1738249477 1 udp 2122260223 192.168.43.1 40678 typ host generation 0
          |b=AS:30
          |a=candidate:1738249477 2 udp 2122260223 192.168.43.1 40678 typ host generation 0
          |a=candidate:211962667 1 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:211962667 2 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:2441410931 1 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=candidate:2441410931 2 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=ice-ufrag:wAYPGvXiff8UghxF8
          |a=ice-pwd:KAo7HueRkuhnYvI3xhT5uVCTc
          |a=ice-options:google-ice
          |a=fingerprint:sha-256 C5:CA:A0:C5:DA:59:2E:79:6D:EF:F3:7F:51:B5:E7:93:95:B0:82:66:3C:8B:34:7B:88:0C:B0:DD:F2:7E:EA:77
          |a=setup:actpass
          |a=mid:data
          |a=sendrecv
          |a=rtcp-mux
          |a=rtpmap:101 google-data/90000
          |a=ssrc:2730398394 cname:GcDTPaBCsTWYVR7S
          |a=ssrc:2730398394 msid:myDataChannel myDataChannel
          |a=ssrc:2730398394 mslabel:myDataChannel
          |a=ssrc:2730398394 label:myDataChannel
        |""".stripMargin //
          .replace("\n", "\r\n")))

      val result = parser.parseSessionDescription().recover { case e @ ParseError(position, traces) => fail(s"\n${parser.formatErrorProblem(e)}: ${parser.formatErrorLine(e)}: ${e.formatTraces}", e) }.get

      result.origin should be(Origin(Some("jdoe"), "2890844526", 2890842807L, NetworkType.IN, AddressType.IP4, InetSocketAddress.createUnresolved("10.47.16.5", 0)))
      result.protocolVersion should be(ProtocolVersion.`0`)
      result.sessionName should be(Some("SDP Seminar"))
      result.sessionInformation should be(Some("A Seminar on the session description protocol"))
      result.descriptionUri should be(Some("http://www.example.com/seminars/sdp.pdf"))
      result.emailAddresses should contain only "j.doe@example.com (Jane Doe)"
      result.phoneNumbers should contain only "+4917624822132"
      result.connectionInformation should be(Some(ConnectionData(NetworkType.IN, AddressType.IP4, InetSocketAddress.createUnresolved("224.2.17.12/127", 0))))
      result.bandwidthInformation should be(Some(BandwidthInformation(BandwidthType.AS, 1024)))
      result.timings should contain only Timing(Some(2873397496L), Some(2873404696L),
        repeatings = Some(RepeatTimes(TimeSpan(604800L, TimeUnit.Days), TimeSpan(3600L), Seq(TimeSpan.ZERO, TimeSpan(90000L, TimeUnit.Minutes)))),
        zoneAdjustments = Seq(TimeZoneAdjustment(0, TimeSpan(0L, TimeUnit.Days)))
      )
      result.sessionAttributes should contain only (PropertyAttribute("recvonly"), ValueAttribute("foo", "bar"))
      result.encryptionKey should be(Some(PromptEncryptionKey))
      result.mediaDescriptions should not be 'empty
      result.mediaDescriptions(0).media should be(Media.audio)
      result.mediaDescriptions(0).connectionInformation should be(Nil)
      result.mediaDescriptions(0).portRange should be(PortRange(49170))
      result.mediaDescriptions(0).protocol should be(MediaTransportProtocol.`RTP/AVP`)
      result.mediaDescriptions(0).mediaAttributes should be(Seq(PropertyAttribute("custom")))
    }

    "parse and render identical" in {
      val sdtext =
        """v=0
          |o=- 5817373415835868156 2 IN IP4 127.0.0.1
          |s=-
          |t=0 0
          |a=group:BUNDLE audio data
          |a=msid-semantic: WMS
          |m=audio 40678 RTP/SAVPF 111 103 104 0 8 106 105 13 126
          |c=IN IP4 192.168.43.47
          |a=rtcp:40678 IN IP4 192.168.0.1
          |a=candidate:1738249477 1 udp 2122260223 192.168.43.47 40678 typ host generation 0
          |a=candidate:1738249477 2 udp 2122260223 192.168.43.47 40678 typ host generation 0
          |a=candidate:211962667 1 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:211962667 2 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:2441410931 1 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=candidate:2441410931 2 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=ice-ufrag:wAYPGvXic8UghxF8
          |a=ice-pwd:KAo7HuggRkuhYvI3xhT5uVCTc
          |a=ice-options:google-ice
          |a=fingerprint:sha-256 C4:CF:A0:C5:DA:59:2E:79:6D:EF:F3:7F:51:B5:E7:93:95:B0:82:66:3C:8B:34:7B:88:0C:B0:DD:F2:7E:EA:77
          |a=setup:actpass
          |a=mid:audio
          |a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level
          |a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
          |a=recvonly
          |a=rtcp-mux
          |a=rtpmap:111 opus/48000/2
          |a=fmtp:111 minptime=10
          |a=rtpmap:103 ISAC/16000
          |a=rtpmap:104 ISAC/32000
          |a=rtpmap:0 PCMU/8000
          |a=rtpmap:8 PCMA/8000
          |a=rtpmap:106 CN/32000
          |a=rtpmap:105 CN/16000
          |a=rtpmap:13 CN/8000
          |a=rtpmap:126 telephone-event/8000
          |a=maxptime:60
          |m=application 40678 RTP/SAVPF 101
          |c=IN IP4 192.168.43.1
          |a=rtcp:40678 IN IP4 192.168.43.1
          |a=candidate:1738249477 1 udp 2122260223 192.168.43.1 40678 typ host generation 0
          |b=AS:30
          |a=candidate:1738249477 2 udp 2122260223 192.168.43.1 40678 typ host generation 0
          |a=candidate:211962667 1 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:211962667 2 udp 2122194687 10.0.4.1 36181 typ host generation 0
          |a=candidate:2441410931 1 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=candidate:2441410931 2 udp 2122129151 172.17.43.1 34456 typ host generation 0
          |a=ice-ufrag:wAYPGvXiff8UghxF8
          |a=ice-pwd:KAo7HueRkuhnYvI3xhT5uVCTc
          |a=ice-options:google-ice
          |a=fingerprint:sha-256 C5:CA:A0:C5:DA:59:2E:79:6D:EF:F3:7F:51:B5:E7:93:95:B0:82:66:3C:8B:34:7B:88:0C:B0:DD:F2:7E:EA:77
          |a=setup:actpass
          |a=mid:data
          |a=sendrecv
          |a=rtcp-mux
          |a=rtpmap:101 google-data/90000
          |a=ssrc:2730398394 cname:GcDTPaBCsTWYVR7S
          |a=ssrc:2730398394 msid:myDataChannel myDataChannel
          |a=ssrc:2730398394 mslabel:myDataChannel
          |a=ssrc:2730398394 label:myDataChannel
          |""".stripMargin //
          .replace("\n", "\r\n")

      val sd = new SessionDescriptionParserImpl(input(sdtext)).parseSessionDescription().get
      SdpRendering.render(sd) should be(sdtext)

    }

  }

  def input(str: String) = new StringBasedParserInput(str)

}

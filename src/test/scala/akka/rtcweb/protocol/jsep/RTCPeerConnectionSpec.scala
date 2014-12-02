package akka.rtcweb.protocol.jsep

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.rtcweb.protocol.jsep.RTCPeerConnection.{ BundlePolicy, PeerConnectionConfiguration, RTCOfferOptions, CreateOffer }
import akka.rtcweb.protocol.sdp._
import akka.testkit.{ TestProbe, DefaultTimeout, TestKit, TestActorRef }
import org.scalatest._
import scala.concurrent.duration._

class RTCPeerConnectionSpec extends TestKit(ActorSystem("RTCPeerConnectionSpec")) with DefaultTimeout with FreeSpecLike with GivenWhenThen with Matchers with BeforeAndAfterAll with Inspectors {

  val listenerProbe = TestProbe()
  val unitRef = TestActorRef[RTCPeerConnection](RTCPeerConnection.props(PeerConnectionConfiguration(Nil, 1, BundlePolicy.`max-compat`)))
  val unit = unitRef.underlyingActor

  override def afterAll() { shutdown() }

  "5.2. Constructing an Offer" - {
    When("""createOffer is called, a new SDP description must be created that includes the functionality specified in
           |[I-D.ietf-rtcweb-rtp-usage].  The exact details of this process are explained below.""".stripMargin)

    "Initial Offers" - {


      When("createOffer is called for the first time, the result is known as the initial offer.")
      unitRef ! CreateOffer(RTCOfferOptions(DtlsSrtpKeyAgreement = false, RtpDataChannels = true))
      lazy val initialOffer = listenerProbe.expectMsgClass(1 second, classOf[SessionDescription])


      info(
        """
          |The first step in generating an initial offer is to generate session-
          |level attributes, as specified in [RFC4566], Section 5.
        """.stripMargin)

      "The first SDP line MUST be \"v=0\", as specified in [RFC4566], Section 5.1" in {
        initialOffer.protocolVersion should be(ProtocolVersion.`0`)
      }

      "The second SDP line MUST be an \"o=\" line, as specified in [RFC4566], Section 5.2." - {
        "The value of the <username> field SHOULD be \"-\"" in {
          initialOffer.origin.username should be(None)
        }
        "The value of the <sess-id> field SHOULD be a cryptographically random number." ignore {
          //todo: cannot be unit-tested :(
        }
        "To ensure uniqueness, this number SHOULD be at least 64 bits long" in {
          initialOffer.origin.`sess-id` should have length 8
        }
        "The value of the <sess-version> field SHOULD be zero" in {
          initialOffer.origin.`sess-version` should be(0)
        }
        "The value of the <nettype> <addrtype> <unicast-address> tuple SHOULD be set to a non-meaningful address, " +
          "such as IN IP4 0.0.0.0, to prevent leaking the local address in this field" in {
          initialOffer.origin.nettype should be(NetworkType.IN)
          initialOffer.origin.addrtype should be(AddressType.IP4)
          initialOffer.origin.`unicast-address`.getHostName should be("0.0.0.0")
        }
      }
      "The third SDP line MUST be a \"s=\" line, as specified in [RFC4566], Section 5.3" - {
        "to match the \"o=\" line, a single dash SHOULD be used as the session name" in {
          initialOffer.sessionName should be(None)
        }

        info("Note that this differs from the advice in [RFC4566] which proposes a single space, but as both \"o=\" and \"s=\" are meaningless, " +
          "having the same meaningless value seems clearer.")

        "Session Information (\"i=\"), URI (\"u=\"), Email Address (\"e=\"), Phone Number (\"p=\"), Bandwidth (\"b=\"), Repeat Times (\"r=\"), and Time Zones (\"z=\") lines are not useful in this context and SHOULD NOT be included." in {
          initialOffer.sessionInformation should be(None)
          initialOffer.descriptionUri should be(None)
          initialOffer.emailAddresses shouldBe empty
          initialOffer.phoneNumbers shouldBe empty
          initialOffer.bandwidthInformation should be(None)
        }

        "Encryption Keys (\"k=\") lines do not provide sufficient security and MUST NOT be included." in {
          initialOffer.encryptionKey should be(None) //todo: can we mix should and must?
        }

        "A \"t=\" line MUST be added, as specified in [RFC4566], Section 5.9; both <start-time> and <stop-time> SHOULD be set to zero, e.g. \"t=0 0\"." in {
          initialOffer.timings should be(Seq(Timing(Some(0L),Some(0L), None, Nil)))
        }
        "An \"a=msid-semantic:WMS\" line MUST be added, as specified in [I-D.ietf-mmusic-msid], Section 4." in {
          initialOffer.sessionAttributes should contain(ValueAttribute("msid-semantic", "WMS")) //todo: can we mix should and must?
        }
      }

      """Each m= section should be generated as specified in [RFC4566],
        |Section 5.14.  For the m= line itself, the following rules MUST be
        |followed:""".stripMargin - {
          """The port value is set to the port of the default ICE candidate for
            |this m= section, but given that no candidates have yet been
            |gathered, the "dummy" port value of 9 (Discard) MUST be used, as
            |indicated in [I-D.ietf-mmusic-trickle-ice], Section 5.1.
            |""".stripMargin in {
            forAll(initialOffer.mediaDescriptions) {
              _.portRange.port should be(9)
            }

          }
          """To properly indicate use of DTLS, the <proto> field MUST be set to
            |"UDP/TLS/RTP/SAVPF", as specified in [RFC5764], Section 8, if the
            |default candidate uses UDP transport, or "TCP/TLS/RTP/SAVPF", as
            |specified in[I-D.nandakumar-mmusic-proto-iana-registration] if the
            |default candidate uses TCP transport.""".stripMargin in {
            forAll(initialOffer.mediaDescriptions) {
              _.protocol shouldBe oneOf(MediaTransportProtocol.`UDP/TLS/RTP/SAVPF`, MediaTransportProtocol.`UDP/TLS/RTP/SAVP`)
            }
          }
      }


      """The m= line MUST be followed immediately by a "c=" line, as specified
        |in [RFC4566], Section 5.7.  Again, as no candidates have yet been
        |gathered, the "c=" line must contain the "dummy" value "IN IP6 ::",
        | as defined in [I-D.ietf-mmusic-trickle-ice], Section 5.1.""".stripMargin in {
        forAll(initialOffer.mediaDescriptions) { md =>
          md.connectionInformation should have size 1
          md.connectionInformation(0).nettype should be(NetworkType.IN)
          md.connectionInformation(0).addrtype should be(AddressType.IP6)
          md.connectionInformation(0).`connection-address` should be(InetSocketAddress.createUnresolved("::",0))

        }
      }

      """Each m= section MUST include the following attribute lines:""" - {

        """An "a=mid" line, as specified in [RFC5888], Section 4.  When
          |generating mid values, it is RECOMMENDED that the values be 3
          |bytes or less, to allow them to efficiently fit into the RTP
          |header extension defined in
          |[I-D.ietf-mmusic-sdp-bundle-negotiation], Section 11.""".stripMargin in {

          ???

        }


      }


    }
  }

}

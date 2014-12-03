package akka.rtcweb.protocol.jsep

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.rtcweb.protocol.ice.{Setup, Fingerprint, IcePwd, IceUfrag}
import akka.rtcweb.protocol.jsep.RTCPeerConnection.{ BundlePolicy, PeerConnectionConfiguration, RTCOfferOptions, CreateOffer }
import akka.rtcweb.protocol.sdp._
import akka.rtcweb.protocol.sdp.grouping.MediaStreamIdentifier
import akka.testkit.{ TestProbe, DefaultTimeout, TestKit, TestActorRef }
import org.scalatest._
import scala.concurrent.duration._

class RTCPeerConnectionSpec extends TestKit(ActorSystem("RTCPeerConnectionSpec")) with DefaultTimeout with FreeSpecLike with GivenWhenThen with Matchers with BeforeAndAfterAll with Inspectors with OptionValues {

  val listenerProbe = TestProbe()
  val unitRef = TestActorRef[RTCPeerConnection](RTCPeerConnection.props(PeerConnectionConfiguration(Nil, 1, BundlePolicy.`max-compat`)))
  val unit = unitRef.underlyingActor

  override def afterAll() {
    shutdown()
  }

  "5.2. Constructing an Offer" - {
    When(
      """createOffer is called, a new SDP description must be created that includes the functionality specified in
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
          initialOffer.timings should be(Seq(Timing(Some(0L), Some(0L), None, Nil)))
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
          forAll(initialOffer.mediaDescriptions)(_.portRange.port should be(9))

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
        forAll(initialOffer.mediaDescriptions) { m =>
          m.connectionInformation should have size 1
          m.connectionInformation(0).nettype should be(NetworkType.IN)
          m.connectionInformation(0).addrtype should be(AddressType.IP6)
          m.connectionInformation(0).`connection-address` should be(InetSocketAddress.createUnresolved("::", 0))

        }
      }

      """Each m= section MUST include the following attribute lines:""" - {

        """An "a=mid" line, as specified in [RFC5888], Section 4.  When
          |generating mid values, it is RECOMMENDED that the values be 3
          |bytes or less, to allow them to efficiently fit into the RTP
          |header extension defined in
          |[I-D.ietf-mmusic-sdp-bundle-negotiation], Section 11.""".stripMargin in {

          forAll(initialOffer.mediaDescriptions) { m =>
            val mid = m.mediaAttributes.collect { case e: MediaStreamIdentifier => e.tag }
            mid should have size 1
            mid.headOption.value should have length 3
          }

        }

        """An "a=rtcp" line, as specified in [RFC3605], Section 2.1,
          |containing the dummy value "9 IN IP6 ::", because no candidates
          |have yet been gathered.""".stripMargin ignore {
          // SRTP is not yet supported by akka-rtcweb
        }

        """An "a=msid" line, as specified in [I-D.ietf-mmusic-msid],
          |Section 2.""".stripMargin ignore {
          // SRTP is not yet supported by akka-rtcweb
        }

        """An "a=sendrecv" line, as specified in [RFC3264], Section 5.1.""" in {
          forAll(initialOffer.mediaDescriptions) { m =>
            val sendrcv = m.mediaAttributes.collectFirst { case e @ PropertyAttribute("sendrecv") => e }
            sendrcv should be(Some(PropertyAttribute("sendrecv")))
          }
        }
        """For each supported codec, "a=rtpmap" and "a=fmtp" lines, as
          |specified in [RFC4566], Section 6.  For audio, the codecs
          |specified in [I-D.ietf-rtcweb-audio], Section 3, MUST be be
          |supported.""".stripMargin in {
          ???
        }

        """If this m= section is for media with configurable frame sizes,
          |e.g. audio, an "a=maxptime" line, indicating the smallest of the
          |maximum supported frame sizes out of all codecs included above, as
          |specified in [RFC4566], Section 6.""".stripMargin ignore {
          // DataChannel does not require configurable streamsizes. TODO: Really?
        }

        """For each primary codec where RTP retransmission should be used, a
          |corresponding "a=rtpmap" line indicating "rtx" with the clock rate
          |of the primary codec and an "a=fmtp" line that references the
          |payload type of the primary codec, as specified in [RFC4588],
          |Section 8.1.""".stripMargin ignore {
          // DataChannel does not require RTP retransmission. TODO: Really?
        }

        """ For each supported FEC mechanism, a corresponding "a=rtpmap" line indicating the desired FEC codec.""" ignore {
          // Neither...
        }

        """"a=ice-ufrag" and "a=ice-passwd" lines, as specified in [RFC5245], Section 15.4.""" in {
          //TODO: ensure that ice ufrag is atleast present at session level
          forAll(initialOffer.mediaDescriptions) { m =>
            val iceUfrag = m.mediaAttributes.collectFirst { case IceUfrag(e) => e }
            iceUfrag.value.size should (be >= 4 and be <= 255)
            val icePwd = m.mediaAttributes.collectFirst { case IcePwd(e) => e }
            icePwd.value.size should (be >= 22 and be <= 255)
          }
        }

        """An "a=ice-options" line, with the "trickle" option,
          |as specified in [I-D.ietf-mmusic-trickle-ice], Section 4.""".stripMargin ignore {
          //TODO: trickle not yet supported. but it should be.
        }

        """An "a=fingerprint" line, as specified in [RFC4572], Section 5; the
          |algorithm used for the fingerprint MUST match that used in the
          |certificate signature.""".stripMargin in {
          forAll(initialOffer.mediaDescriptions) { m =>
            val fingerprint = m.mediaAttributes.collectFirst { case e @ Fingerprint(_, _) => e }
            fingerprint.value.fingerprint
          }
        }

        """An "a=setup" line, as specified in [RFC4145], Section 4, and
          |      clarified for use in DTLS-SRTP scenarios in [RFC5763], Section 5.
          |      The role value in the offer MUST be "actpass".""".stripMargin in {
          forAll(initialOffer.mediaDescriptions) { m =>
            val setupRole = m.mediaAttributes.collectFirst { case Setup(role) => role }
            setupRole.value should be(Setup.Role.actpass)
          }



        }

      }

    }

  }

}

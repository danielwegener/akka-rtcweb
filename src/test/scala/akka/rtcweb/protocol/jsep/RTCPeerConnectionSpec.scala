package akka.rtcweb.protocol.jsep

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.rtcweb.protocol.ice.{Setup, Fingerprint, IcePwd, IceUfrag}
import akka.rtcweb.protocol.jsep.RTCPeerConnection.{ BundlePolicy, PeerConnectionConfiguration, RTCOfferOptions, CreateOffer }
import akka.rtcweb.protocol.sdp._
import akka.rtcweb.protocol.sdp.grouping.{Semantics, Group, MediaStreamIdentifier}
import akka.rtcweb.protocol.sdp.sctp.Sctpmap
import akka.testkit.{ TestProbe, DefaultTimeout, TestKit, TestActorRef }
import org.scalatest._
import scala.concurrent.duration._

class RTCPeerConnectionSpec extends TestKit(ActorSystem("RTCPeerConnectionSpec")) with DefaultTimeout with FreeSpecLike with GivenWhenThen with Matchers with BeforeAndAfterAll with Inspectors with OptionValues {

  import Inspectors._

  val listenerProbe = TestProbe()
  val bundlePolicy: BundlePolicy = BundlePolicy.`max-bundle`
  val unitRef = TestActorRef[RTCPeerConnection](RTCPeerConnection.props(PeerConnectionConfiguration(Nil, 1, bundlePolicy)))
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
      val rtcOfferOptions = RTCOfferOptions(DtlsSrtpKeyAgreement = false, RtpDataChannels = true)
      unitRef ! CreateOffer(rtcOfferOptions)
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
        |followed:""".stripMargin - { """The port value is set to the port of the default ICE candidate for
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
          info("no SRTP support yet")
        }

        """An "a=msid" line, as specified in [I-D.ietf-mmusic-msid],
          |Section 2.""".stripMargin ignore {
          info("no SRTP support yet")
        }

        """An "a=sendrecv" line, as specified in [RFC3264], Section 5.1.""" in {
          forAll(initialOffer.mediaDescriptions) { m =>
            val sendrcv = m.mediaAttributes.collectFirst { case e @PropertyAttribute("sendrecv") => e }
            sendrcv should be(Some(PropertyAttribute("sendrecv")))
          }
        }
        """For each supported codec, "a=rtpmap" and "a=fmtp" lines, as
          |specified in [RFC4566], Section 6.  For audio, the codecs
          |specified in [I-D.ietf-rtcweb-audio], Section 3, MUST be be
          |supported.""".stripMargin ignore {
          info("no RTP support yet")
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
            val fingerprint = m.mediaAttributes.collectFirst { case e @Fingerprint(_, _) => e }
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

        """An "a=rtcp-mux" line, as specified in [RFC5761], Section 5.1.1.""" ignore {
          info("no RTCP support yet")
        }

        """An "a=rtcp-rsize" line, as specified in [RFC5506], Section 5.""" ignore {
          info("no RTCP support yet")
        }

        """For each supported RTP header extension, an "a=extmap" line, as
          |specified in [RFC5285], Section 5.  The list of header extensions
          |that SHOULD/MUST be supported is specified in
          |[I-D.ietf-rtcweb-rtp-usage], Section 5.2.  [TODO: ensure that
          |urn:ietf:params:rtp-hdrext:sdes:mid appears either there or here]
          |Any header extensions that require encryption MUST be specified as
          |indicated in [RFC6904], Section 4.""".stripMargin ignore {
          info("no RTP support yet")
        }

        """For each supported RTCP feedback mechanism, an "a=rtcp-fb"
          |mechanism, as specified in [RFC4585], Section 4.2.  The list of
          |RTCP feedback mechanisms that SHOULD/MUST be supported is
          |specified in [I-D.ietf-rtcweb-rtp-usage], Section 5.1.""".stripMargin ignore {
          info("no RTP support yet")
        }

        """An "a=ssrc" line, as specified in [RFC5576], Section 4.1,
          |indicating the SSRC to be used for sending media, along with the
          |mandatory "cname" source attribute, as specified in Section 6.1,
          |indicating the CNAME for the source.  The CNAME must be generated
          |in accordance with [RFC7022].  [OPEN ISSUE: How are CNAMEs
          |specified for MSTs?  Are they randomly generated for each
          |MediaStream?  If so, can two MediaStreams be synced?  See:
          |https://github.com/rtcweb-wg/jsep/issues/4]""".stripMargin ignore {
          info("no RTP support yet")
        }

        """If RTX is supported for this media type, another "a=ssrc" line
          |with the RTX SSRC, and an "a=ssrc-group" line, as specified in
          |[RFC5576], section 4.2, with semantics set to "FID" and including
          |the primary and RTX SSRCs.""".stripMargin ignore {
          info("RTX not supported")
        }

        """If FEC is supported for this media type, another "a=ssrc" line
          |with the FEC SSRC, and an "a=ssrc-group" line, as specified in
          |[RFC5576], section 4.2, with semantics set to "FEC" and including
          |the primary and FEC SSRCs.""".stripMargin ignore {
          info("FEC not supported")
        }

        """[OPEN ISSUE: Handling of a=imageattr]""" ignore {
          info("wait for draft-ietf-rtcweb-jsep-09 :)")
        }

        """If the BUNDLE policy for this PeerConnection is set to "max-
          |bundle", and this is not the first m= section, or the BUNDLE
          |policy is set to "balanced", and this is not the first m= section
          |for this media type, an "a=bundle-only" line.""" in {
          if (bundlePolicy == BundlePolicy.`max-bundle` || bundlePolicy == BundlePolicy.balanced) {
            forAll(initialOffer.mediaDescriptions.tail) { m =>
              m.mediaAttributes should contain(PropertyAttribute("bundle-only"))
            }
          } else {
            info(s"In this case, the bundle-policy was $bundlePolicy")
          }
        }

      }

      """Lastly, if a data channel has been created, a m= section MUST be
        |generated for data.  The <media> field MUST be set to "application"
        |and the <proto> field MUST be set to "UDP/TLS/SCTP" if the default
        |candidate uses UDP transport, or "TCP/TLS/SCTP" if the default
        |candidate uses TCP transport [I-D.ietf-mmusic-sctp-sdp].  The "fmt"
        |value MUST be set to the SCTP port number, as specified in
        |Section 4.1.  [TODO: update this to use a=sctp-port, as indicated in
        |the latest data channel docs]""".stripMargin in {
        if (true /* TODO: ensure create dataChannel has been called */) {
          val m = initialOffer.mediaDescriptions.find(_.media == Media.application)
          /* FIXME: This seem to be an inconsistency between draft-ietf-rtcweb-jsep-08 and draft-ietf-mmusic-sctp-sdp-08
            * draft-ietf-mmusic-sctp-sdp-08 names the protocol identifier 'DTLS/SCTP'. Since it is in a newer version, we will use this */

          val sctpmap = m.value.mediaAttributes.collectFirst{ case e: Sctpmap => e }
          m.value.protocol shouldBe MediaTransportProtocol.`DTLS/SCTP`
          m.value.fmt shouldBe List(sctpmap.value.number.toString)


        } else {
          info(s"In this case, the bundle-policy was $bundlePolicy")
        }
      }

      """Once all m= sections have been generated, a session-level "a=group"
        |attribute MUST be added as specified in [RFC5888].  This attribute
        |MUST have semantics "BUNDLE", and MUST include the mid identifiers of
        |each m= section.  The effect of this is that the browser offers all
        |m= sections as one BUNDLE group.  However, whether the m= sections
        |are bundle-only or not depends on the BUNDLE policy.""".stripMargin - {

        val mediaIdentifiersFromGroup = initialOffer.sessionAttributes.collectFirst{ case e@Group(Semantics.UnknownSemanticsExtension("BUNDLE"), mids) => mids }
        val midsFromMedia = initialOffer.mediaDescriptions.flatMap{ md => md.mediaAttributes.collectFirst { case MediaStreamIdentifier(tag) => tag} }

      }

      """Attributes other than the ones specified above MAY be included,
        |except for the following attributes which are specifically
        |incompatible with the requirements of [I-D.ietf-rtcweb-rtp-usage],
        |and MUST NOT be included:
        |o "a=crypto"
        |o  "a=key-mgmt"
        |o  "a=ice-lite"
        |""".stripMargin in {
        forAll(initialOffer.mediaDescriptions) {
          _ should not matchPattern {
            case PropertyAttribute("crypto") =>
            case PropertyAttribute("key-mgmt") =>
            case PropertyAttribute("ice-lite") =>
          }
        }
      }

      """Note that when BUNDLE is used, any additional attributes that are
        |added MUST follow the advice in [I-D.ietf-mmusic-sdp-mux-attributes]
        |on how those attributes interact with BUNDLE.""" ignore {
        //TODO: implement
      }


    }




    "5.2.2.  Subsequent Offers" - {

      info("""When createOffer is called a second (or later) time, or is called
        |after a local description has already been installed, the processing
        |is somewhat different than for an initial offer.""".stripMargin)

        """If the initial offer was not applied using setLocalDescription,
        |meaning the PeerConnection is still in the "stable" state, the steps
        |for generating an initial offer should be followed, subject to the
        |following restriction:""".stripMargin in {

          val rtcOfferOptions = RTCOfferOptions(DtlsSrtpKeyAgreement = false, RtpDataChannels = true)
          unitRef ! CreateOffer(rtcOfferOptions)
          lazy val initialOffer = listenerProbe.expectMsgClass(1 second, classOf[SessionDescription])
          unitRef ! CreateOffer(rtcOfferOptions)
          lazy val subsequentOffer = listenerProbe.expectMsgClass(1 second, classOf[SessionDescription])

          """The fields of the "o=" line MUST stay the same except for the
            |<session-version> field, which MUST increment if the session
            |description changes in any way, including the addition of ICE
            |candidates.""".stripMargin in {
            if (initialOffer != subsequentOffer) {
              subsequentOffer.origin.`sess-id` shouldBe initialOffer.origin.`sess-id`
              subsequentOffer.origin.`unicast-address` shouldBe initialOffer.origin.`unicast-address`
              subsequentOffer.origin.addrtype shouldBe initialOffer.origin.addrtype
              subsequentOffer.origin.nettype shouldBe initialOffer.origin.nettype
              subsequentOffer.origin.username shouldBe initialOffer.origin.username
              subsequentOffer.origin.`sess-version` shouldNot be(initialOffer.origin.`sess-version` + 1)
            }
          }

      }

      """ If the initial offer was applied using setLocalDescription, but an
        |answer from the remote side has not yet been applied, meaning the
        |PeerConnection is still in the "local-offer" state, an offer is
        |generated by following the steps in the "stable" state above, along
        |with these exceptions:""".stripMargin in {
          //TODO: continue
      }

    }

  }

  }



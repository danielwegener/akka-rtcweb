package akka.rtcweb.protocol.jsep

import akka.actor.ActorSystem
import akka.rtcweb.protocol.jsep.RTCPeerConnection.{ BundlePolicy, PeerConnectionConfiguration, RTCOfferOptions, CreateOffer }
import akka.rtcweb.protocol.sdp._
import akka.testkit.{ TestProbe, DefaultTimeout, TestKit, TestActorRef }
import org.scalatest._
import scala.concurrent.duration._

class RTCPeerConnectionSpec extends TestKit(ActorSystem("RTCPeerConnectionSpec")) with DefaultTimeout with FreeSpecLike with GivenWhenThen with Matchers with BeforeAndAfterAll {

  import MustMatchers._

  val listenerProbe = TestProbe()
  val unitRef = TestActorRef[RTCPeerConnection](RTCPeerConnection.props(PeerConnectionConfiguration(Nil, 1, BundlePolicy.`max-compat`)))
  val unit = unitRef.underlyingActor

  override def afterAll() { shutdown() }

  "5.2. Constructing an Offer" - {
    info("""When createOffer is called, a new SDP description must be created that includes the functionality specified in
           |[I-D.ietf-rtcweb-rtp-usage].  The exact details of this process are explained below.""".stripMargin)

    "Initial Offers" in {

      When("createOffer is called for the first time, the result is known as the initial offer.")

      unitRef ! CreateOffer(RTCOfferOptions(DtlsSrtpKeyAgreement = false, RtpDataChannels = true))
      val initialOffer = listenerProbe.expectMsgClass(1 second, classOf[SessionDescription])

      info(
        """
          |The first step in generating an initial offer is to generate session-
          |level attributes, as specified in [RFC4566], Section 5.
        """.stripMargin)

      Then("The first SDP line MUST be \"v=0\", as specified in [RFC4566], Section 5.1")
      initialOffer.protocolVersion should be(ProtocolVersion.`0`)

      "The second SDP line MUST be an \"o=\" line, as specified in [RFC4566], Section 5.2." in {
        Then("The value of the <username> field SHOULD be \"-\"")
        initialOffer.origin.username should be(None)
        Then("The value of the <sess-id> field SHOULD be a cryptographically random number.")
        //todo: cannot be unit-tested :(
        Then("To ensure uniqueness, this number SHOULD be at least 64 bits long")
        initialOffer.origin.`sess-id` should have length 8
        Then("The value of the <sess-version> field SHOULD be zero")
        initialOffer.origin.`sess-version` should be(0)
        Then("The value of the <nettype> <addrtype> <unicast-address> tuple SHOULD be set to a non-meaningful address, " +
          "such as IN IP4 0.0.0.0, to prevent leaking the local address in this field")
        initialOffer.origin.nettype should be(NetworkType.IN)
        initialOffer.origin.addrtype should be(AddressType.IP4)
        initialOffer.origin.`unicast-address`.getHostName should be("0.0.0.0")
      }
      "The third SDP line MUST be a \"s=\" line, as specified in [RFC4566], Section 5.3" in {
        Then("to match the \"o=\" line, a single dash SHOULD be used as the session name")
        initialOffer.sessionName should be(None)
        info("Note that this differs from the advice in [RFC4566] which proposes a single space, but as both \"o=\" and \"s=\" are meaningless, " +
          "having the same meaningless value seems clearer.")
        Then("Session Information (\"i=\"), URI (\"u=\"), Email Address (\"e=\"), Phone Number (\"p=\"), Bandwidth (\"b=\"), Repeat Times (\"r=\"), and Time Zones (\"z=\") lines are not useful in this context and SHOULD NOT be included.")
        initialOffer.sessionInformation should be(None)
        initialOffer.descriptionUri should be(None)
        initialOffer.emailAddresses shouldBe empty
        initialOffer.phoneNumbers shouldBe empty
        initialOffer.bandwidthInformation should be(None)

        Then("Encryption Keys (\"k=\") lines do not provide sufficient security and MUST NOT be included.")
        initialOffer.encryptionKey must be(None)

        Then("A \"t=\" line MUST be added, as specified in [RFC4566], Section 5.9; both <start-time> and <stop-time> SHOULD be set to zero, e.g. \"t=0 0\".")
        initialOffer.timings should be(Seq(Timing(Some(0L),Some(0L), None, Nil)))
        Then("An \"a=msid-semantic:WMS\" line MUST be added, as specified in [I-D.ietf-mmusic-msid], Section 4.")
        initialOffer.sessionAttributes must contain(ValueAttribute("msid-semantic", "WMS"))

      }

    }
  }

}

package akka.rtcweb.protocol.jsep

import akka.actor.{ ActorSystem }
import akka.rtcweb.protocol.jsep.RTCPeerConnection.{ BundlePolicy, PeerConnectionConfiguration, RTCOfferOptions, CreateOffer }
import akka.rtcweb.protocol.sdp.{ ProtocolVersion, SessionDescription }
import akka.testkit.{ TestProbe, DefaultTimeout, TestKit, TestActorRef }
import org.scalatest._
import scala.concurrent.duration._

class RTCPeerConnectionSpec extends TestKit(ActorSystem("RTCPeerConnectionSpec")) with DefaultTimeout with FreeSpecLike with GivenWhenThen with Matchers with BeforeAndAfterAll {

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

    }
  }

}

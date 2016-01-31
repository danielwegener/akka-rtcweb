package akka.rtcweb.protocol.jsep

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.rtcweb.protocol.RtcWebSDPRenderer
import akka.rtcweb.protocol.jsep.RTCPeerConnection._
import akka.rtcweb.protocol.sdp.sctp.Sctpmap
import akka.rtcweb.protocol.sdp._
import akka.stream.{ ActorMaterializerSettings, ActorMaterializer }
import akka.stream.scaladsl.Flow
import org.specs2.execute.Result
import org.specs2.mutable.Specification

import akka.rtcweb.protocol.ice.{ Setup, Fingerprint, IcePwd, IceUfrag }
import akka.rtcweb.protocol.sdp.grouping.{ Semantics, Group, MediaStreamIdentifier }
import akka.testkit._
import scala.concurrent.duration._
import org.specs2._

/**
 * @see [[https://tools.ietf.org/html/draft-ietf-rtcweb-jsep-09]]
 */
class RTCPeerConnectionSpec extends Specification with TestKitBase with DefaultTimeout {

  // Run tests sequentially
  sequential

  lazy val initialOffer = {
    unitRef ! CreateDataChannel(Flow.apply, "my-data-channel")
    val rtcOfferOptions = RTCOfferOptions(DtlsSrtpKeyAgreement = false, RtpDataChannels = false)
    listenerProbe.send(unitRef, CreateOffer(rtcOfferOptions))
    lazy val initialRTCOffer = listenerProbe.expectMsgClass(1 second, classOf[RTCSessionDescription.offer])
    initialRTCOffer.sessionDescription
  }

  override implicit val system: ActorSystem = ActorSystem("RTCPeerConnectionSpec")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))

  val listenerProbe = TestProbe()
  val dataChannelProbe = TestProbe()
  val bundlePolicy: BundlePolicy = BundlePolicy.`max-bundle`
  val iceAgentProbe = TestProbe()
  val unitRef = TestActorRef[RTCPeerConnection](RTCPeerConnection.props(PeerConnectionConfiguration(Vector.empty, 1, bundlePolicy), iceAgentProbe.ref))
  val unit = unitRef.underlyingActor
  val sdpRenderer = new RtcWebSDPRenderer

  def theFirstSDPLineMUSTBe = renderedInitialOffer(0) must be("v=0")

  def theSecondSDPLineMustStartWith = renderedInitialOffer(1) must startWith("o=")

  def renderedInitialOffer = sdpRenderer.render(initialOffer).split('\n')

  def theSecondSDPLineShould = initialOffer.origin.username should be(None)

  def initialOfferSessionIdIsRandom = skipped("cannot be unit-tested :(")

  def initialOfferSessionIdLength = initialOffer.origin.`sess-id`.length should be >= 8

  def initialOfferSessionVersion = initialOffer.origin.`sess-version` should equalTo(0L)

  def initialOfferOriginNetTuple = {
    (initialOffer.origin.nettype should be(NetworkType.IN)) and
      (initialOffer.origin.addrtype should be(AddressType.IP4)) and
      (initialOffer.origin.`unicast-address`.getHostName === "0.0.0.0" /*read: should*/ )
  }

  def theThirdSDPLineMustBeAnSLine = renderedInitialOffer(2) must startWith("s=")

  def sessionNameShouldBeNone = initialOffer.sessionName should be(None)

  def sessionInformationShouldBeNone = {
    (initialOffer.sessionInformation should be(None)) and
      (initialOffer.descriptionUri should be(None)) and
      (initialOffer.emailAddresses should beEmpty) and
      (initialOffer.phoneNumbers should beEmpty) and
      (initialOffer.bandwidthInformation should be(None))
  }

  def encryptionKeysMustNotExist = initialOffer.encryptionKey must beNone

  def timingMustBeAdded = initialOffer.timings should be(Seq(Timing(Some(0L), Some(0L), None, Nil)))

  def msidSemanticAttributeMustBeAdded = initialOffer.sessionAttributes must contain(ValueAttribute("msid-semantic", "WMS"))

  def mediaPortTrickleMustBe9 = {
    (initialOffer.mediaDescriptions must not be empty) and
      forall(initialOffer.mediaDescriptions)(_.portRange.port must beEqualTo(9))
  }

  def protocolMustBeUdpTls = forall(initialOffer.mediaDescriptions) {
    _.protocol must be oneOf (MediaTransportProtocol.`UDP/TLS/RTP/SAVPF`, MediaTransportProtocol.`UDP/TLS/RTP/SAVP`)
  }

  def cLineMustContainDummyValue = forall(initialOffer.mediaDescriptions) { m =>
    m.connectionInformation must beSome
    m.connectionInformation.get.nettype must be(NetworkType.IN)
    m.connectionInformation.get.addrtype must be(AddressType.IP6)
    m.connectionInformation.get.`connection-address` must be(InetSocketAddress.createUnresolved("::", 0))
  }

  def midValueShouldFitIntoRTPHeaderExtension =
    forall(initialOffer.mediaDescriptions) { m =>
      val mids = m.mediaAttributes.collect { case e: MediaStreamIdentifier => e.tag }
      (mids should not(beEmpty)) and
        forall(mids) { mid =>
          mid must have size 3 // should, but keyword doesnt work here. o.x
        }
    }

  def mAttributesContainsrtcpLine = pending("no SRTP support yet")

  def mAttributesContainsMsidLine = pending("no SRTP support yet")

  def mAttributesContainsSendrcvLine = forall(initialOffer.mediaDescriptions) { m =>
    val sendrcv = m.mediaAttributes.collectFirst { case e @ PropertyAttribute("sendrecv") => e }
    sendrcv must be(Some(PropertyAttribute("sendrecv")))
  }

  def mAttributeContainsForeachSupportedCodecRtpmapAndFmtp = pending("no RTP support yet")

  def mMaxptimeSmallestFrameSize = pending("no maxptime support yet (irrelevant for data channels)")

  def mRtpmapForEachPrimaryCodecWithRetransmission = pending("no codec support yet (irrelevant for data channels)")

  def mRtpmapForEachSupportecFEC = pending("no FEC support yet (irrelevant for data channels)")

  def iceUfragAndIcePasswdLines = forall(initialOffer.mediaDescriptions) { m =>
    val iceUfrag = m.mediaAttributes.collectFirst { case IceUfrag(e) => e }
    iceUfrag must beSome
    iceUfrag.get must have size (be >= 4 and be <= 255)
    val icePwd = m.mediaAttributes.collectFirst { case IcePwd(e) => e }
    icePwd must beSome
    icePwd.get must have size (be >= 22 and be <= 255)
  }

  def iceUfragAndIcePasswdAtSessionLevel = pending("ensure that ice ufrag is atleast present at session level")

  def iceOptionsLineWithTrickleOption = pending("trickle not yet supported. but it should be.")

  def fingerprintLine = forall(initialOffer.mediaDescriptions) { m =>
    val fingerprint = m.mediaAttributes.collectFirst { case e @ Fingerprint(_, _) => e }
    fingerprint must beSome
    pending("todo: check it matches the used in the certificate signature")
  }

  def setupLineAttribute = forall(initialOffer.mediaDescriptions) { m =>
    val setupRole = m.mediaAttributes.collectFirst { case Setup(role) => role }
    setupRole must beSome
    setupRole.get must be(Setup.Role.actpass)
  }

  def rtcpMuxAttribute = pending("no RTCP support yet")

  def rtcpRsizeAttribute = pending("no RTCP support yet")

  def extmapAttributeForSupportedRTPHeaderExtension = pending("no RTCP support yet")

  def rctpFbAttributeForSupportedFeedbackMechanism = pending("no RTCP support yet")

  def ssrcAttribute = pending("no RTCP support yet")

  def ssrcAttributeAndSsrcGroupLineForRTX = pending("no RTX support yet")

  def ssrcForFEC = pending("no FEC support yet")

  def imageAttrOpenIssue = pending("Waiting for the working group to fix that :)")

  def maxBundlePolicy: Result = {
    if (bundlePolicy == BundlePolicy.`max-bundle` || bundlePolicy == BundlePolicy.balanced) {
      forall(initialOffer.mediaDescriptions.tail) { m =>
        m.mediaAttributes should contain(PropertyAttribute("bundle-only"))
      }
    } else {
      skipped(s"In this case, the bundle-policy was $bundlePolicy")
    }
  }

  def dataMediaDescriptor: Result = {

    if (true /* TODO: ensure create dataChannel has been called */ ) {
      val m = initialOffer.mediaDescriptions.find(_.media == Media.application)
      /* FIXME: This seem to be an inconsistency between draft-ietf-rtcweb-jsep-09 and draft-ietf-mmusic-sctp-sdp-08
        * draft-ietf-mmusic-sctp-sdp-08 names the protocol identifier 'DTLS/SCTP'. Since it is in a newer version, we will use this */
      m must beSome
      val sctpmap = m.get.mediaAttributes.collectFirst { case e: Sctpmap => e }
      m.get.protocol must be(MediaTransportProtocol.`UDP/DTLS/SCTP`)
      m.get.fmt must be(List(sctpmap.get.number.toString))

    } else {
      skipped(s"In this case, there has no data-channel be created")
    }
  }

  def dataMediaDescriptorFMTPortNumber = todo

  def sessionLevelGroupAttribute = {
    val mediaIdentifiersFromGroup = initialOffer.sessionAttributes.collectFirst { case e @ Group(Semantics.UnknownSemanticsExtension("BUNDLE"), mids) => mids }
    val midsFromMedia = initialOffer.mediaDescriptions.flatMap { md => md.mediaAttributes.collectFirst { case MediaStreamIdentifier(tag) => tag } }
    todo
  }

  def additionalAttributesMustFollowAdvice = todo

  def excludeIncompatibleAttributes = forall(initialOffer.mediaDescriptions) { m =>
    m should not be (PropertyAttribute("crypto"))
    m should not be (PropertyAttribute("key-mgmt"))
    m should not be (PropertyAttribute("ice-lite"))
  }

  /* cleanup */
  step(shutdown())

  //TODO: translate
  /**
   * "5.2.2.  Subsequent Offers" - {
   *
   * info("""When createOffer is called a second (or later) time, or is called
   * |after a local description has already been installed, the processing
   * |is somewhat different than for an initial offer.""".stripMargin)
   *
   * """If the initial offer was not applied using setLocalDescription,
   * |meaning the PeerConnection is still in the "stable" state, the steps
   * |for generating an initial offer should be followed, subject to the
   * |following restriction:""".stripMargin in {
   *
   * val rtcOfferOptions = RTCOfferOptions(DtlsSrtpKeyAgreement = false, RtpDataChannels = true)
   * unitRef ! CreateOffer(rtcOfferOptions)
   * lazy val initialOffer = listenerProbe.expectMsgClass(1 second, classOf[SessionDescription])
   * unitRef ! CreateOffer(rtcOfferOptions)
   * lazy val subsequentOffer = listenerProbe.expectMsgClass(1 second, classOf[SessionDescription])
   *
   * """The fields of the "o=" line MUST stay the same except for the
   * |<session-version> field, which MUST increment if the session
   * |description changes in any way, including the addition of ICE
   * |candidates.""".stripMargin in {
   * if (initialOffer != subsequentOffer) {
   * subsequentOffer.origin.`sess-id` shouldBe initialOffer.origin.`sess-id`
   * subsequentOffer.origin.`unicast-address` shouldBe initialOffer.origin.`unicast-address`
   * subsequentOffer.origin.addrtype shouldBe initialOffer.origin.addrtype
   * subsequentOffer.origin.nettype shouldBe initialOffer.origin.nettype
   * subsequentOffer.origin.username shouldBe initialOffer.origin.username
   * subsequentOffer.origin.`sess-version` shouldNot be(initialOffer.origin.`sess-version` + 1)
   * }
   * }
   *
   * }
   *
   * """ If the initial offer was applied using setLocalDescription, but an
   * |answer from the remote side has not yet been applied, meaning the
   * |PeerConnection is still in the "local-offer" state, an offer is
   * |generated by following the steps in the "stable" state above, along
   * |with these exceptions:""".stripMargin in {
   * //TODO: continue
   * }
   *
   * }
   */

}


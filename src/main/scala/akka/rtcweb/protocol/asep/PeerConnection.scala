package akka.rtcweb.protocol.asep

import java.net.InetAddress

import akka.actor.Actor
import akka.rtcweb.protocol.asep.PeerConnection.{ CreateOffer, PeerConnectionConfiguration }
import akka.rtcweb.protocol.sdp.SessionDescription

object PeerConnection {

  /**
   * By specifying a policy from the list below, the application can control how aggressively it
   * will try to BUNDLE media streams together.  The set of available
   * policies is as follows:
   */
  sealed trait BundlePolicy
  object BundlePolicy {

    /**
     * The application will BUNDLE all media streams of the same
     * type together.  That is, if there are multiple audio and multiple
     * video MediaStreamTracks attached to a PeerConnection, all but the
     * first audio and video tracks will be marked as bundle-only, and
     * candidates will only be gathered for N media streams, where N is
     * the number of distinct media types.  When talking to a non-BUNDLE-
     * aware endpoint, only the non-bundle-only streams will be
     * negotiated.  This policy balances desire to multiplex with the
     * need to ensure basic audio and video still works in legacy cases.
     * Data channels will be in a separate bundle group.
     */
    case object balanced extends BundlePolicy

    /**
     *
     * max-bundle:  The application will BUNDLE all of its media streams,
     * including data channels, on a single transport.  All streams other
     * than the first will be marked as bundle-only.  This policy aims to
     * minimize candidate gathering and maximize multiplexing, at the
     * cost of less compatibility with legacy endpoints.
     */
    case object `max-bundle` extends BundlePolicy

    /**
     * The application will offer BUNDLE, but mark none of its
     * streams as bundle-only.  This policy will allow all streams to be
     * received by non-BUNDLE-aware endpoints, but require separate
     * candidates to be gathered for each media stream.
     */
    case object `max-compat` extends BundlePolicy
  }

  final case class StunServerDescription(address: InetAddress, credentials: Option[Nothing] = None)

  final case class PeerConnectionConfiguration(
    stunServers: Seq[StunServerDescription],
    iceCandidatePoolSize: Int,
    bundlePolicy: BundlePolicy)

  /**
   *  The createOffer method takes as a parameter an RTCOfferOptions
   * object.  Special processing is performed when generating a SDP
   * description if the following constraints are present.
   *
   * @param OfferToReceiveAudio If the "OfferToReceiveAudio" option is specified, with an integer
   * value of N, the offer MUST include N non-rejected m= sections with
   * media type "audio", even if fewer than N audio MediaStreamTracks have
   * been added to the PeerConnection.  This allows the offerer to receive
   * audio, including multiple independent streams, even when not sending
   * it; accordingly, the directional attribute on the audio m= sections
   * without associated MediaStreamTracks MUST be set to recvonly.  If
   * this option is specified in the case where at least N audio
   * MediaStreamTracks have already been added to the PeerConnection, or N
   * non-rejected m= sections with media type "audio" would otherwise be
   * generated, it has no effect.  For backwards compatibility, a value of
   * "true" is interpreted as equivalent to N=1.
   *
   * @param OfferToReceiveVideo If the "OfferToReceiveVideo" option is specified, with an integer
   * value of N, the offer MUST include N non-rejected m= sections with
   * media type "video", even if fewer than N video MediaStreamTracks have
   * been added to the PeerConnection.  This allows the offerer to receive
   * video, including multiple independent streams, even when not sending
   * it; accordingly, the directional attribute on the video m= sections
   * without associated MediaStreamTracks MUST be set to recvonly.  If
   * this option is specified in the case where at least N video
   * MediaStreamTracks have already been added to the PeerConnection, or N
   * non-rejected m= sections with media type "video" would otherwise be
   * generated, it has no effect.  For backwards compatibility, a value of
   * "true" is interpreted as equivalent to N=1.
   *
   * @param VoiceActivityDetection  If the "VoiceActivityDetection" option is specified, with a value of
   * "true", the offer MUST indicate support for silence suppression in
   * the audio it receives by including comfort noise ("CN") codecs for
   * each offered audio codec, as specified in [RFC3389], Section 5.1,
   * except for codecs that have their own internal silence suppression
   * support.  For codecs that have their own internal silence suppression
   * support, the appropriate fmtp parameters for that codec MUST be
   * specified to indicate that silence suppression for received audio is
   * desired.  For example, when using the Opus codec, the "usedtx=1"
   * parameter would be specified in the offer.  This option allows the
   * endpoint to significantly reduce the amount of audio bandwidth it
   * receives, at the cost of some fidelity, depending on the quality of
   * the remote VAD algorithm.
   *
   * @param IceRestart  If the "IceRestart" option is specified, with a value of "true", the
   * offer MUST indicate an ICE restart by generating new ICE ufrag and
   * pwd attributes, as specified in RFC5245, Section 9.1.1.1.  If this
   * option is specified on an initial offer, it has no effect (since a
   * new ICE ufrag and pwd are already generated).  This option is useful
   * for reestablishing connectivity in cases where failures are detected.
   *
   */
  final case class RTCOfferOptions(
    OfferToReceiveAudio: Option[Int],
    OfferToReceiveVideo: Option[Int],
    VoiceActivityDetection: Boolean = false,
    IceRestart: Boolean = false,
    DtlsSrtpKeyAgreement: Boolean,
    RtpDataChannels: Boolean)

  final case class CreateAnswerOptions()

  /**
   * Session description objects (RTCSessionDescription) may be of type
   * "offer", "pranswer", and "answer".  These types provide information
   * as to how the description parameter should be parsed, and how the
   * media state should be changed.
   */
  sealed trait RTCSessionDescription
  sealed trait HasSDPPayload extends RTCSessionDescription { def sessionDescription: SessionDescription }

  object RTCSessionDescription {

    /**
     *  "offer" indicates that a description should be parsed as an offer;
     * said description may include many possible media configurations.  A
     * description used as an "offer" may be applied anytime the
     * PeerConnection is in a stable state, or as an update to a previously
     * supplied but unanswered "offer".
     */
    final case class offer(sessionDescription: SessionDescription) extends RTCSessionDescription with HasSDPPayload

    /**
     * "pranswer" indicates that a description should be parsed as an
     * answer, but not a final answer, and so should not result in the
     * freeing of allocated resources.  It may result in the start of media
     * transmission, if the answer does not specify an inactive media
     * direction.  A description used as a "pranswer" may be applied as a
     * response to an "offer", or an update to a previously sent "pranswer".
     */
    final case class pranswer(sessionDescription: SessionDescription) extends RTCSessionDescription with HasSDPPayload

    /**
     *  "answer" indicates that a description should be parsed as an answer,
     * the offer-answer exchange should be considered complete, and any
     * resources (decoders, candidates) that are no longer needed can be
     * released.  A description used as an "answer" may be applied as a
     * response to a "offer", or an update to a previously sent "pranswer".
     *
     * The only difference between a provisional and final answer is that
     * the final answer results in the freeing of any unused resources that
     * were allocated as a result of the offer.  As such, the application
     * can use some discretion on whether an answer should be applied as
     * provisional or final, and can change the type of the session
     * description as needed.  For example, in a serial forking scenario, an
     * application may receive multiple "final" answers, one from each
     * remote endpoint.  The application could choose to accept the initial
     * answers as provisional answers, and only apply an answer as final
     * when it receives one that meets its criteria (e.g. a live user
     * instead of voicemail).
     */
    final case class answer(sessionDescription: SessionDescription) extends RTCSessionDescription with HasSDPPayload

    /**
     *  "rollback" is a special session description type implying that the
     * state machine should be rolled back to the previous state, as
     * described in Section 4.1.4.2.  The contents MUST be empty.
     */
    case object rollback extends RTCSessionDescription

  }

  sealed trait PeerConnectionMessage

  /**
   * Session descriptions generated by createOffer must be immediately
   * usable by setLocalDescription; if a system has limited resources
   * (e.g. a finite number of decoders), createOffer should return an
   * offer that reflects the current state of the system, so that
   * setLocalDescription will succeed when it attempts to acquire those
   * resources.  Because this method may need to inspect the system state
   * to determine the currently available resources, it may be implemented
   * as an async operation.
   *
   * Calling this method may do things such as generate new ICE
   * credentials, but does not result in candidate gathering, or cause
   * media to start or stop flowing.
   *
   * The createOffer method generates a blob of SDP that contains a
   * [RFC3264] offer with the supported configurations for the session,
   * including descriptions of the local MediaStreams attached to this
   * PeerConnection, the codec/RTP/RTCP options supported by this
   * implementation, and any candidates that have been gathered by the ICE
   * Agent.  An options parameter may be supplied to provide additional
   * control over the generated offer.  This options parameter should
   * allow for the following manipulations to be performed:
   *
   * - To indicate support for a media type even if no MediaStreamTracks
   * of that type have been added to the session (e.g., an audio call
   * that wants to receive video.)
   * - To trigger an ICE restart, for the purpose of reestablishing
   * connectivity.
   *
   * In the initial offer, the generated SDP will contain all desired
   * functionality for the session (functionality that is supported but
   * not desired by default may be omitted); for each SDP line, the
   * generation of the SDP will follow the process defined for generating
   * an initial offer from the document that specifies the given SDP line.
   * The exact handling of initial offer generation is detailed in
   * Section 5.2.1 below.
   *
   * In the event createOffer is called after the session is established,
   * createOffer will generate an offer to modify the current session
   * based on any changes that have been made to the session, e.g. adding
   * or removing MediaStreams, or requesting an ICE restart.  For each
   * existing stream, the generation of each SDP line must follow the
   * process defined for generating an updated offer from the RFC that
   * specifies the given SDP line.  For each new stream, the generation of
   * the SDP must follow the process of generating an initial offer, as
   * mentioned above.  If no changes have been made, or for SDP lines that
   * are unaffected by the requested changes, the offer will only contain
   * the parameters negotiated by the last offer-answer exchange.  The
   * exact handling of subsequent offer generation is detailed in
   * Section 5.2.2. below.
   *
   * Session descriptions generated by createOffer must be immediately
   * usable by setLocalDescription; if a system has limited resources
   * (e.g. a finite number of decoders), createOffer should return an
   * offer that reflects the current state of the system, so that
   * setLocalDescription will succeed when it attempts to acquire those
   * resources.  Because this method may need to inspect the system state
   * to determine the currently available resources, it may be implemented
   * as an async operation.
   *
   * Calling this method may do things such as generate new ICE
   * credentials, but does not result in candidate gathering, or cause
   * media to start or stop flowing.
   *
   */
  final case class CreateOffer(options: RTCOfferOptions) extends PeerConnectionMessage

  /**
   * The createAnswer method generates a blob of SDP that contains a
   * [RFC3264] SDP answer with the supported configuration for the session
   * that is compatible with the parameters supplied in the most recent
   * call to setRemoteDescription, which MUST have been called prior to
   * calling createAnswer.  Like createOffer, the returned blob contains
   * descriptions of the local MediaStreams attached to this
   * PeerConnection, the codec/RTP/RTCP options negotiated for this
   * session, and any candidates that have been gathered by the ICE Agent.
   * An options parameter may be supplied to provide additional control
   * over the generated answer.
   *
   * As an answer, the generated SDP will contain a specific configuration
   * that specifies how the media plane should be established; for each
   * SDP line, the generation of the SDP must follow the process defined
   * for generating an answer from the document that specifies the given
   * SDP line.  The exact handling of answer generation is detailed in
   * Section 5.3. below.
   *
   * Session descriptions generated by createAnswer must be immediately
   * usable by setLocalDescription; like createOffer, the returned
   * description should reflect the current state of the system.  Because
   * this method may need to inspect the system state to determine the
   * currently available resources, it may need to be implemented as an
   * async operation.
   *
   * Calling this method may do things such as generate new ICE
   * credentials, but does not trigger candidate gathering or change media
   * state.
   *
   */
  final case class CreateAnswer(options: CreateAnswerOptions) extends PeerConnectionMessage

  /**
   *   The setLocalDescription method instructs the PeerConnection to apply
   * the supplied SDP blob as its local configuration.  The type field
   * indicates whether the blob should be processed as an offer,
   * provisional answer, or final answer; offers and answers are checked
   * differently, using the various rules that exist for each SDP line.
   *
   * This API changes the local media state; among other things, it sets
   * up local resources for receiving and decoding media.  In order to
   * successfully handle scenarios where the application wants to offer to
   * change from one media format to a different, incompatible format, the
   * PeerConnection must be able to simultaneously support use of both the
   * old and new local descriptions (e.g. support codecs that exist in
   * both descriptions) until a final answer is received, at which point
   * the PeerConnection can fully adopt the new local description, or roll
   * back to the old description if the remote side denied the change.
   *
   * This API indirectly controls the candidate gathering process.  When a
   * local description is supplied, and the number of transports currently
   * in use does not match the number of transports needed by the local
   * description, the PeerConnection will create transports as needed and
   * begin gathering candidates for them.
   *
   * If setRemoteDescription was previous called with an offer, and
   * setLocalDescription is called with an answer (provisional or final),
   * and the media directions are compatible, and media are available to
   * send, this will result in the starting of media transmission.
   */
  final case class SetLocalDescription(todo: Nothing = ???)

}

class PeerConnection(private val config: PeerConnectionConfiguration) extends Actor {

  override def receive: Receive = {
    case CreateOffer(options) => ??? //new SessionDescription(protocolVersion = ProtocolVersion.`0`)

  }

}

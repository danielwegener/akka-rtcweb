package akka.rtcweb.protocol.ice

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, TestKitBase}
import org.specs2.Specification

class RFC5245Spec extends Specification with TestKitBase {


  override implicit val system: ActorSystem = ActorSystem("RFC5245Spec")

  val listener = TestProbe()

  val unitRef = TestActorRef(Props(new IceAgent(listener.ref)))

  def is =
    s2"""
        |4.  Sending the Initial Offer
        |
        |   In order to send the initial offer in an offer/answer exchange, an
        |   agent must (1) gather candidates, (2) prioritize them, (3) eliminate
        |   redundant candidates, (4) choose default candidates, and then (5)
        |   formulate and send the SDP offer.  All but the last of these five
        |   steps differ for full and lite implementations.
        |
        |4.1.  Full Implementation Requirements
        |
        |4.1.1.  Gathering Candidates
        |
        |   An agent gathers candidates when it believes that communication is
        |   imminent.  An offerer can do this based on a user interface cue, or
        |   based on an explicit request to initiate a session.  Every candidate
        |
        |
        |
        |
        |Rosenberg                    Standards Track                   [Page 19]
        |
        |RFC 5245                           ICE                        April 2010
        |
        |
        |   is a transport address.  It also has a type and a base.  Four types
        |   are defined and gathered by this specification -- host candidates,
        |   server reflexive candidates, peer reflexive candidates, and relayed
        |   candidates.  The server reflexive candidates are gathered using STUN
        |   or TURN, and relayed candidates are obtained through TURN.  Peer
        |   reflexive candidates are obtained in later phases of ICE, as a
        |   consequence of connectivity checks.  The base of a candidate is the
        |   candidate that an agent must send from when using that candidate.
        |
        |4.1.1.1.  Host Candidates
        |
        |   The first step is to gather host candidates.  Host candidates are
        |   obtained by binding to ports (typically ephemeral) on a IP address
        |   attached to an interface (physical or virtual, including VPN
        |   interfaces) on the host.
        |
        |   For each UDP media stream the agent wishes to use, the agent SHOULD
        |   obtain a candidate for each component of the media stream on each IP
        |   address that the host has.  It obtains each candidate by binding to a
        |   UDP port on the specific IP address.  A host candidate (and indeed
        |   every candidate) is always associated with a specific component for
        |   which it is a candidate.  Each component has an ID assigned to it,
        |   called the component ID.  For RTP-based media streams, the RTP itself
        |   has a component ID of 1, and RTCP a component ID of 2.  If an agent
        |   is using RTCP, it MUST obtain a candidate for it.  If an agent is
        |   using both RTP and RTCP, it would end up with 2*K host candidates if
        |   an agent has K IP addresses.
        |
        |   The base for each host candidate is set to the candidate itself.
        |
        |4.1.1.2.  Server Reflexive and Relayed Candidates
        |
        |   Agents SHOULD obtain relayed candidates and SHOULD obtain server
        |   reflexive candidates.  These requirements are at SHOULD strength to
        |   allow for provider variation.  Use of STUN and TURN servers may be
        |   unnecessary in closed networks where agents are never connected to
        |   the public Internet or to endpoints outside of the closed network.
        |   In such cases, a full implementation would be used for agents that
        |   are dual stack or multihomed, to select a host candidate.  Use of
        |   TURN servers is expensive, and when ICE is being used, they will only
        |   be utilized when both endpoints are behind NATs that perform address
        |   and port dependent mapping.  Consequently, some deployments might
        |   consider this use case to be marginal, and elect not to use TURN
        |   servers.  If an agent does not gather server reflexive or relayed
        |   candidates, it is RECOMMENDED that the functionality be implemented
        |   and just disabled through configuration, so that it can be re-enabled
        |   through configuration if conditions change in the future.
        |
        |
        |
        |
        |Rosenberg                    Standards Track                   [Page 20]
        |
        |RFC 5245                           ICE                        April 2010
        |
        |
        |   If an agent is gathering both relayed and server reflexive
        |   candidates, it uses a TURN server.  If it is gathering just server
        |   reflexive candidates, it uses a STUN server.
        |
        |   The agent next pairs each host candidate with the STUN or TURN server
        |   with which it is configured or has discovered by some means.  If a
        |   STUN or TURN server is configured, it is RECOMMENDED that a domain
        |   name be configured, and the DNS procedures in [RFC5389] (using SRV
        |   records with the "stun" service) be used to discover the STUN server,
        |   and the DNS procedures in [RFC5766] (using SRV records with the
        |   "turn" service) be used to discover the TURN server.
        |
        |   This specification only considers usage of a single STUN or TURN
        |   server.  When there are multiple choices for that single STUN or TURN
        |   server (when, for example, they are learned through DNS records and
        |   multiple results are returned), an agent SHOULD use a single STUN or
        |   TURN server (based on its IP address) for all candidates for a
        |   particular session.  This improves the performance of ICE.  The
        |   result is a set of pairs of host candidates with STUN or TURN
        |   servers.  The agent then chooses one pair, and sends a Binding or
        |   Allocate request to the server from that host candidate.  Binding
        |   requests to a STUN server are not authenticated, and any ALTERNATE-
        |   SERVER attribute in a response is ignored.  Agents MUST support the
        |   backwards compatibility mode for the Binding request defined in
        |   [RFC5389].  Allocate requests SHOULD be authenticated using a long-
        |   term credential obtained by the client through some other means.
        |
        |   Every Ta milliseconds thereafter, the agent can generate another new
        |   STUN or TURN transaction.  This transaction can either be a retry of
        |   a previous transaction that failed with a recoverable error (such as
        |   authentication failure), or a transaction for a new host candidate
        |   and STUN or TURN server pair.  The agent SHOULD NOT generate
        |   transactions more frequently than one every Ta milliseconds.  See
        |   Section 16 for guidance on how to set Ta and the STUN retransmit
        |   timer, RTO.
        |
        |   The agent will receive a Binding or Allocate response.  A successful
        |   Allocate response will provide the agent with a server reflexive
        |   candidate (obtained from the mapped address) and a relayed candidate
        |   in the XOR-RELAYED-ADDRESS attribute.  If the Allocate request is
        |   rejected because the server lacks resources to fulfill it, the agent
        |   SHOULD instead send a Binding request to obtain a server reflexive
        |   candidate.  A Binding response will provide the agent with only a
        |   server reflexive candidate (also obtained from the mapped address).
        |   The base of the server reflexive candidate is the host candidate from
        |   which the Allocate or Binding request was sent.  The base of a
        |   relayed candidate is that candidate itself.  If a relayed candidate
        |
        |
        |
        |
        |Rosenberg                    Standards Track                   [Page 21]
        |
        |RFC 5245                           ICE                        April 2010
        |
        |
        |   is identical to a host candidate (which can happen in rare cases),
        |   the relayed candidate MUST be discarded.
        |
        |4.1.1.3.  Computing Foundations
      """.stripMargin






}

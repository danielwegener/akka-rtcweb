package akka.rtcweb.protocol.ice

import akka.rtcweb.protocol.sdp.ExtensionAttribute

import java.net.InetSocketAddress

sealed trait IceExtensionAttribute extends ExtensionAttribute

/**
 * The attribute contains a connection-address and port for each
 * component.  The ordering of components is irrelevant.  However, a
 * value MUST be present for each component of a media stream.  This
 * attribute MUST be included in an offer by a controlling agent for a
 * media stream that is Completed, and MUST NOT be included in any other
 * case.
 */
final case class RemoteCandidates(candidates: Map[Int, InetSocketAddress]) extends IceExtensionAttribute {
  override def key: String = "remote-candidates"
}

object Candidate {
  type ExtensionAttribute = Tuple2[String, String]
}

/**
 * @param foundation It is an
 * identifier that is equivalent for two candidates that are of the
 * same type, share the same base, and come from the same STUN
 * server.  The foundation is used to optimize ICE performance in the
 * Frozen algorithm.
 * @param componentId  is a positive integer between 1 and 256 that
 * identifies the specific component of the media stream for which
 * this is a candidate.  It MUST start at 1 and MUST increment by 1
 * for each component of a particular candidate.  For media streams
 * based on RTP, candidates for the actual RTP media MUST have a
 * component ID of 1, and candidates for RTCP MUST have a component
 * ID of 2.  Other types of media streams that require multiple
 * components MUST develop specifications that define the mapping of
 * components to component IDs.  See Section 14 for additional
 * discussion on extending ICE to new media streams.
 * @param transport indicates the transport protocol for the candidate.
 * This specification only defines UDP.  However, extensibility is
 * provided to allow for future transport protocols to be used with
 * ICE, such as TCP or the Datagram Congestion Control Protocol
 * (DCCP) [RFC4340].
 * @param priority  is a positive integer between 1 and (2**31 - 1).
 * @param connectionAddress It is the
 * IP address of the candidate, allowing for IPv4 addresses, IPv6
 * addresses, and fully qualified domain names (FQDNs).  When parsing
 * this field, an agent can differentiate an IPv4 address and an IPv6
 * address by presence of a colon in its value - the presence of a
 * colon indicates IPv6.  An agent MUST ignore candidate lines that
 * include candidates with IP address versions that are not supported
 * or recognized.  An IP address SHOULD be used, but an FQDN MAY be
 * used in place of an IP address.  In that case, when receiving an
 * offer or answer containing an FQDN in an a=candidate attribute,
 * the FQDN is looked up in the DNS first using an AAAA record
 * (assuming the agent supports IPv6), and if no result is found or
 * the agent only supports IPv4, using an A.  If the DNS query
 * returns more than one IP address, one is chosen, and then used for
 * the remainder of ICE processing. The port is within this InetSocketAddress.
 * @param candidateType  encodes the type of candidate.  This specification
 * defines the values "host", "srflx", "prflx", and "relay" for host,
 * server reflexive, peer reflexive, and relayed candidates,
 * respectively.  The set of candidate types is extensible for the
 * future.
 * @param relayConnectionAddress convey transport addresses related to the
 * candidate, useful for diagnostics and other purposes. <rel-addr>
 * and <rel-port> MUST be present for server reflexive, peer
 * reflexive, and relayed candidates.  If a candidate is server or
 * peer reflexive, <rel-addr> and <rel-port> are equal to the base
 * for that server or peer reflexive candidate.  If the candidate is
 * relayed, <rel-addr> and <rel-port> is equal to the mapped address
 * in the Allocate response that provided the client with that
 * relayed candidate (see Appendix B.3 for a discussion of its
 * purpose).  If the candidate is a host candidate, <rel-addr> and
 * <rel-port> MUST be omitted.
 * @param extensionAttributes The candidate attribute can itself be extended.  The grammar allows
 * for new name/value pairs to be added at the end of the attribute.  An
 * implementation MUST ignore any name/value pairs it doesn't
 * understand.
 */
final case class Candidate(
    foundation: String,
    componentId: Int,
    transport: Transport,
    priority: Priority,
    connectionAddress: InetSocketAddress,
    candidateType: CandidateType,
    relayConnectionAddress: Option[InetSocketAddress],
    extensionAttributes: Seq[Candidate.ExtensionAttribute]) extends IceExtensionAttribute {
  override def key: String = "candidate"
}

final case class Priority(priority: Long) extends AnyVal

sealed trait Transport
object Transport {
  final case class UnknownTransportExtension(token: String) extends Transport
  case object UDP extends Transport
}

sealed trait CandidateType

object CandidateType {

  final case class UnknownCandidateType(token: String) extends CandidateType
  case object host extends CandidateType
  case object srflx extends CandidateType
  case object prflx extends CandidateType
  case object relay extends CandidateType
}

final case class IcePwd(password: String) extends IceExtensionAttribute {
  override def key: String = "ice-pwd"
}
final case class IceUfrag(ufrag: String) extends IceExtensionAttribute {
  override def key: String = "ice-ufrag"
}
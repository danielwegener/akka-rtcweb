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
  case object TCP extends Transport
}

sealed trait CandidateType

object CandidateType {

  final case class UnknownCandidateType(token: String) extends CandidateType
  /** host: Candidate that is learned from local interfaces */
  case object HostCandidate extends CandidateType
  /** srflx: Candidate that is learned from a STUN server */
  case object ServerReflexiveCandidate extends CandidateType
  /** prflx: Candidate that is learned from a peer receiving a connection check. */
  case object PeerReflexiveCandidate extends CandidateType
  /** relay: Candidate that is obtained from a TURN relay. **/
  case object RelayCandidate extends CandidateType
}

/**
 *  The "ice-pwd" attributes can appear at either the
 * session-level or media-level.  When present in both, the value in the
 * media-level takes precedence.  Thus, the value at the session-level
 * is effectively a default that applies to all media streams, unless
 * overridden by a media-level value.  Whether present at the session or
 * media-level, there MUST be an ice-pwd attribute for
 * each media stream.  If two media streams have identical ice-ufrag's,
 * they MUST have identical ice-pwd's.
 *
 * The ice-ufrag and ice-pwd attributes MUST be chosen randomly at the
 * beginning of a session.  The ice-pwd attribute MUST contain
 * at least 128 bits of randomness.  This means that the ice-pwd attribute will be at least
 * 22 characters long, since the grammar for these attributes
 * allows for 6 bits of randomness per character.  The attributes MAY be
 * longer than 22 characters, respectively, of course, up to 256
 * characters.  The upper limit allows for buffer sizing in
 * implementations.  Its large upper limit allows for increased amounts
 * of randomness to be added over time.
 */
final case class IcePwd(password: String) extends IceExtensionAttribute {
  override def key: String = "ice-pwd"
}

/**
 * The "ice-ufrag" attribute can appear at either the
 * session-level or media-level.  When present in both, the value in the
 * media-level takes precedence.  Thus, the value at the session-level
 * is effectively a default that applies to all media streams, unless
 * overridden by a media-level value.  Whether present at the session or
 * media-level, there MUST be an ice-ufrag attribute for
 * each media stream.  If two media streams have identical ice-ufrag's,
 * they MUST have identical ice-pwd's.
 *
 * The ice-ufrag attributes MUST be chosen randomly at the
 * beginning of a session.  The ice-ufrag attribute MUST contain at
 * least 24 bits of randomness.  This means that the ice-ufrag
 * attribute will be at least 4 characters long, since the grammar for these attributes
 * allows for 6 bits of randomness per character.  The attributes MAY be
 * longer than 4, of course, up to 256
 * characters.  The upper limit allows for buffer sizing in
 * implementations.  Its large upper limit allows for increased amounts
 * of randomness to be added over time.
 */
final case class IceUfrag(ufrag: String) extends IceExtensionAttribute {
  override def key: String = "ice-ufrag"
}

/**
 *  A fingerprint is represented in SDP as an attribute (an 'a' line).
 * It consists of the name of the hash function used, followed by the
 * hash value itself.  The hash value is represented as a sequence of
 * uppercase hexadecimal bytes, separated by colons.  The number of
 * bytes is defined by the hash function.  (This is the syntax used by
 * openssl and by the browsers' certificate managers.  It is different
 * from the syntax used to represent hash values in, e.g., HTTP digest
 * authentication [18], which uses unseparated lowercase hexadecimal
 * bytes.  It was felt that consistency with other applications of
 * fingerprints was more important.)
 * @see [[https://tools.ietf.org/html/rfc4572#section-5]]
 */
final case class Fingerprint(hashFunction: HashFunction, fingerprint: String) extends IceExtensionAttribute {
  override def key: String = "fingerprint"
}

sealed trait HashFunction

object HashFunction {
  final case class UnknownHashFunction(token: String) extends HashFunction
  case object `sha-1` extends HashFunction
  case object `sha-224` extends HashFunction
  case object `sha-256` extends HashFunction
  case object `sha-384` extends HashFunction
  case object `sha-512` extends HashFunction
  case object `md5` extends HashFunction
  case object `md2` extends HashFunction
}

/**
 * The 'setup' attribute indicates which of the end points should
 * initiate the TCP connection establishment (i.e., send the initial TCP
 * SYN). The 'setup' attribute is charset-independent and can be a
 * session-level or a media-level attribute.
 */
final case class Setup(role: Setup.Role) extends IceExtensionAttribute {
  override def key: String = "setup"
}

object Setup {

  sealed trait Role

  object Role {
    /** The endpoint will initiate an outgoing connection. */
    case object active extends Role
    /** The endpoint will accept an incoming connection. */
    case object passive extends Role
    /**  The endpoint is willing to accept an incoming connection or to initiate an outgoing connection. */
    case object actpass extends Role
    /** The endpoint does not want the connection to be established for the time being. */
    case object holdconn extends Role
  }
}

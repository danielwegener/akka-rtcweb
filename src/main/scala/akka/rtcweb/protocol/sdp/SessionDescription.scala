package akka.rtcweb.protocol.sdp

import java.net.InetSocketAddress

import scala.collection.immutable.Seq

/**
 * Session description
 * @param protocolVersion v=  (protocol version)
 * @param origin o=  (originator and session identifier)
 * @param sessionName s=  (session name) The "s=" field is the textual session name.  There MUST be one and
 * only one "s=" field per session description.  The "s=" field MUST NOT
 * be empty and SHOULD contain ISO 10646 characters (but see also the
 * "a=charset" attribute).  If a session has no meaningful name, the
 * value "s= " SHOULD be used (i.e., a single space as the session
 * name).
 *
 * @param sessionInformation  i=* (session information) The "i=" field provides textual information about the session.  There
 * MUST be at most one session-level "i=" field per session description,
 * and at most one "i=" field per media.  If the "a=charset" attribute
 * is present, it specifies the character set used in the "i=" field.
 * If the "a=charset" attribute is not present, the "i=" field MUST
 * contain ISO 10646 characters in UTF-8 encoding.
 *
 * A single "i=" field MAY also be used for each media definition.  In
 * media definitions, "i=" fields are primarily intended for labelling
 * media streams.  As such, they are most likely to be useful when a
 * single session has more than one distinct media stream of the same
 * media type.  An example would be two different whiteboards, one for
 * slides and one for feedback and questions.
 *
 * The "i=" field is intended to provide a free-form human-readable
 * description of the session or the purpose of a media stream.  It is
 * not suitable for parsing by automata.
 *
 * @param descriptionUri u=*  (URI of description) A URI is a Uniform Resource Identifier as used by WWW clients [7].
 * The URI should be a pointer to additional information about the
 * session.  This field is OPTIONAL, but if it is present it MUST be
 * specified before the first media field.  No more than one URI field
 * is allowed per session description.
 * @param emailAddresses e=* (email address)
 * @param phoneNumbers p=* (phone number)
 * @param connectionInformation  {{{c=*}}} (connection information -- not required if included in
 * all media)
 * @param bandwidthInformation {{{b=*}}} (zero or more bandwidth information lines)
 * @param timings One or more timings
 * @param encryptionKey {{{k=*}}} (encryption key)
 * @param sessionAttributes {{{a=*}}} (zero or more session attribute lines)
 */
final case class SessionDescription(
    protocolVersion: ProtocolVersion = ProtocolVersion.`0`,
    origin: Origin,
    sessionName: Option[String] = None,
    sessionInformation: Option[String] = None,
    descriptionUri: Option[String] = None,
    emailAddresses: Seq[String] = Nil,
    phoneNumbers: Seq[String] = Nil,
    connectionInformation: Option[ConnectionData] = None,
    bandwidthInformation: Option[BandwidthInformation] = None,
    timings: Seq[Timing] = Nil,
    encryptionKey: Option[EncryptionKey] = None,
    sessionAttributes: Seq[Attribute] = Nil,
    mediaDescriptions: Seq[MediaDescription] = Nil) {

  require(connectionInformation.isDefined || mediaDescriptions.nonEmpty && mediaDescriptions.forall(_.connectionInformation.nonEmpty),
    """A session description MUST contain either at least one connectionInformation in
            | each media description or a single connectionInformation at the session level.
            | It MAY contain a single session-level connectionInformation and additional connectionInformation
            | field(s) per media description, in which case the per-media values
            | override the session-level settings for the respective media.""".stripMargin.stripLineEnd)

}

sealed trait ProtocolVersion { final def value = productPrefix; def productPrefix: String }
object ProtocolVersion {
  case object `0` extends ProtocolVersion
}

/**
 * Origin ("o=")
 *
 * {{{o=<username> <sess-id> <sess-version> <nettype> <addrtype>      <unicast-address>}}}
 * @param username is the user's login on the originating host, or it is "-"
 * if the originating host does not support the concept of user IDs.
 * The <username> MUST NOT contain spaces.
 * @param `sess-id` is a numeric string such that the tuple of <username>,
 * <sess-id>, <nettype>, <addrtype>, and <unicast-address> forms a
 * globally unique identifier for the session.  The method of
 * <sess-id> allocation is up to the creating tool, but it has been
 * suggested that a Network Time Protocol (NTP) format timestamp be
 * used to ensure uniqueness [13].
 * @param `sess-version`  is a version number for this session description.  Its
 * usage is up to the creating tool, so long as <sess-version> is
 * increased when a modification is made to the session data.  Again,
 * it is RECOMMENDED that an NTP format timestamp is used.
 * @param nettype is a text string giving the type of network.  Initially
 * "IN" is defined to have the meaning "Internet", but other values
 * MAY be registered in the future (see Section 8).
 * @param addrtype  is a text string giving the type of the address that
 * follows.  Initially "IP4" and "IP6" are defined, but other values
 * MAY be registered in the future (see Section 8).
 * @param `unicast-address` is the address of the machine from which the
 * session was created.  For an address type of IP4, this is either
 * the fully qualified domain name of the machine or the dotted-
 * decimal representation of the IP version 4 address of the machine.
 * For an address type of IP6, this is either the fully qualified
 * domain name of the machine or the compressed textual
 * representation of the IP version 6 address of the machine.  For
 * both IP4 and IP6, the fully qualified domain name is the form that
 * SHOULD be given unless this is unavailable, in which case the
 * globally unique address MAY be substituted.  A local IP address
 * MUST NOT be used in any context where the SDP description might
 * leave the scope in which the address is meaningful (for example, a
 * local address MUST NOT be included in an application-level
 * referral that might leave the scope).
 */
final case class Origin(
  username: Option[String],
  `sess-id`: String,
  `sess-version`: Long,
  nettype: NetworkType = NetworkType.IN,
  addrtype: AddressType,
  `unicast-address`: InetSocketAddress)

sealed trait NetworkType
object NetworkType {

  /**
   * The Internet
   */
  case object IN extends NetworkType
}
sealed trait AddressType
object AddressType {
  case object IP4 extends AddressType
  case object IP6 extends AddressType
}

/**
 * {{{b=<bwtype>:<bandwidth>}}}
 *
 * This OPTIONAL field denotes the proposed bandwidth to be used by the
 * session or media.  The <bwtype> is an alphanumeric modifier giving
 * the meaning of the <bandwidth> figure.  Two values are defined in
 * this specification, but other values MAY be registered in the future
 * (see Section 8 and [21], [25]):
 *
 * Note that CT gives a total bandwidth figure for all the media at all
 * sites.  AS gives a bandwidth figure for a single media at a single
 * site, although there may be many sites sending simultaneously.
 *
 * A prefix "X-" is defined for <bwtype> names.  This is intended for
 * experimental purposes only.  For example:
 * b=X-YZ:128
 *
 * Use of the "X-" prefix is NOT RECOMMENDED: instead new modifiers
 * SHOULD be registered with IANA in the standard namespace.  SDP
 * parsers MUST ignore bandwidth fields with unknown modifiers.
 * Modifiers MUST be alphanumeric and, although no length limit is
 * given, it is recommended that they be short.
 *
 * The <bandwidth> is interpreted as kilobits per second by default.
 * The definition of a new <bwtype> modifier MAY specify that the
 * bandwidth is to be interpreted in some alternative unit (the "CT" and
 * "AS" modifiers defined in this memo use the default units).
 *
 */
final case class BandwidthInformation(
  bwtype: BandwidthType,
  bandwidth: Int)

sealed trait BandwidthType
object BandwidthType {

  /**
   * If the bandwidth of a session or media in a session is different
   * from the bandwidth implicit from the scope, a "b=CT:..." line
   * SHOULD be supplied for the session giving the proposed upper limit
   * to the bandwidth used (the "conference total" bandwidth).  The
   * primary purpose of this is to give an approximate idea as to
   * whether two or more sessions can coexist simultaneously.  When
   * using the CT modifier with RTP, if several RTP sessions are part
   * of the conference, the conference total refers to total bandwidth
   * of all RTP sessions.
   */
  case object CT extends BandwidthType

  /**
   * The bandwidth is interpreted to be application specific (it will
   * be the application's concept of maximum bandwidth).  Normally,
   * this will coincide with what is set on the application's "maximum
   * bandwidth" control if applicable.  For RTP-based applications, AS
   * gives the RTP "session bandwidth" as defined in Section 6.2 of
   * [19].
   */
  case object AS extends BandwidthType

  final case class Experimental(name: String) extends BandwidthType

}

sealed trait TimeDescription

/**
 * t=  (time the session is active)
 *  The "t=" lines specify the start and stop times for a session.
 * Multiple "t=" lines MAY be used if a session is active at multiple
 * irregularly spaced times; each additional "t=" line specifies an
 * additional period of time for which the session will be active.  If
 * the session is active at regular times, an "r=" line (see below)
 * should be used in addition to, and following, a "t=" line -- in which
 * case the "t=" line specifies the start and stop times of the repeat
 * sequence.
 *
 * The first and second sub-fields give the start and stop times,
 * respectively, for the session.  These values are the decimal
 * representation of Network Time Protocol (NTP) time values in seconds
 * since 1900 [13].  To convert these values to UNIX time, subtract
 * decimal 2208988800.
 *
 * If the {{{<stop-time>}}} is set to zero, then the session is not bounded,
 * though it will not become active until after the {{{<start-time>}}}.  If
 * the {{{<start-time>}}} is also zero, the session is regarded as permanent.
 *
 */
final case class Timing(startTime: Option[Long],
  stopTime: Option[Long],
  repeatings: Option[RepeatTimes] = None,
  zoneAdjustments: Seq[TimeZoneAdjustment] = Nil) extends TimeDescription

/**
 * {{{r=* (zero or more repeat times)
 *  r=<repeat interval> <active duration> <offsets from start-time>}}}
 *     "r=" fields specify repeat times for a session.  For example, if a
 * session is active at 10am on Monday and 11am on Tuesday for one hour
 * each week for three months, then the <start-time> in the
 * corresponding "t=" field would be the NTP representation of 10am on
 * the first Monday, the <repeat interval> would be 1 week, the <active
 * duration> would be 1 hour, and the offsets would be zero and 25
 * hours.  The corresponding "t=" field stop time would be the NTP
 * representation of the end of the last session three months later.  By
 * default, all fields are in seconds, so the "r=" and "t=" fields might
 * be the following:
 * {{{
 * t=3034423619 3042462419
 * r=604800 3600 0 90000
 * }}}
 *
 *  To make description more compact, times may also be given in units of
 * days, hours, or minutes.  The syntax for these is a number
 * immediately followed by a single case-sensitive character.
 * Fractional units are not allowed -- a smaller unit should be used
 * instead.  The following unit specification characters are allowed:
 *
 * {{{
 * d - days (86400 seconds)
 * h - hours (3600 seconds)
 * m - minutes (60 seconds)
 * s - seconds (allowed for completeness)
 * }}}
 *
 *
 */
final case class RepeatTimes(
  repeatInterval: TimeSpan,
  activeDuration: TimeSpan,
  offsetFromStartTime: Seq[TimeSpan] = Nil) extends TimeDescription

object RepeatTimes {
  //def apply(repeatInterval: TimeSpan, activeDuration: TimeSpan, offsetFromStartTime: TimeSpan*): RepeatTimes =
  //apply(repeatInterval, activeDuration, offsetFromStartTime.toVector)
}

final case class TimeSpan(length: Long, timeUnit: TimeUnit = TimeUnit.Seconds)

object TimeSpan {
  val ZERO = TimeSpan(0L)
}

sealed trait TimeUnit
object TimeUnit {
  case object Days extends TimeUnit
  case object Hours extends TimeUnit
  case object Minutes extends TimeUnit
  case object Seconds extends TimeUnit
}

/**
 * {{{z=<adjustment time> <offset> <adjustment time> <offset> ....}}}
 * To schedule a repeated session that spans a change from daylight
 * saving time to standard time or vice versa, it is necessary to
 * specify offsets from the base time.  This is required because
 * different time zones change time at different times of day, different
 * countries change to or from daylight saving time on different dates,
 * and some countries do not have daylight saving time at all.
 *
 * Thus, in order to schedule a session that is at the same time winter
 * and summer, it must be possible to specify unambiguously by whose
 * time zone a session is scheduled.  To simplify this task for
 * receivers, we allow the sender to specify the NTP time that a time
 * zone adjustment happens and the offset from the time when the session
 * was first scheduled.  The "z=" field allows the sender to specify a
 * list of these adjustment times and offsets from the base time.
 *
 * An example might be the following:
 *
 * {{{ z=2882844526 -1h 2898848070 0}}}
 *
 * This specifies that at time 2882844526, the time base by which the
 * session's repeat times are calculated is shifted back by 1 hour, and
 * that at time 2898848070, the session's original time base is
 * restored.  Adjustments are always relative to the specified start
 * time -- they are not cumulative.  Adjustments apply to all "t=" and
 * "r=" lines in a session description.
 *
 * If a session is likely to last several years, it is expected that the
 * session announcement will be modified periodically rather than
 * transmit several years' worth of adjustments in one session
 * announcement.
 *
 */
final case class TimeZoneAdjustment(base: Long, adjustment: TimeSpan)

/**
 * {{{
 *   k=<method>
 * k=<method>:<encryption key>
 * }}}
 *
 *  If transported over a secure and trusted channel, the Session
 * Description Protocol MAY be used to convey encryption keys.  A simple
 * mechanism for key exchange is provided by the key field ("k="),
 * although this is primarily supported for compatibility with older
 * implementations and its use is NOT RECOMMENDED.  Work is in progress
 * to define new key exchange mechanisms for use with SDP [27] [28], and
 * it is expected that new applications will use those mechanisms.
 *
 * A key field is permitted before the first media entry (in which case
 * it applies to all media in the session), or for each media entry as
 * required.  The format of keys and their usage are outside the scope
 * of this document, and the key field provides no way to indicate the
 * encryption algorithm to be used, key type, or other information about
 * the key: this is assumed to be provided by the higher-level protocol
 * using SDP.  If there is a need to convey this information within SDP,
 * the extensions mentioned previously SHOULD be used.  Many security
 * protocols require two keys: one for confidentiality, another for
 * integrity.  This specification does not support transfer of two keys.
 *
 *
 * The key field MUST NOT be used unless it can be guaranteed that the
 * SDP is conveyed over a secure and trusted channel.  An example of
 * such a channel might be SDP embedded inside an S/MIME message or a
 * TLS-protected HTTP session.  It is important to ensure that the
 * secure channel is with the party that is authorised to join the
 * session, not an intermediary: if a caching proxy server is used, it
 * is important to ensure that the proxy is either trusted or unable to
 * access the SDP.
 */
sealed trait EncryptionKey

/**
 * {{{k=clear:<encryption key>}}}
 * The encryption key is included untransformed in this key field.
 * This method MUST NOT be used unless it can be guaranteed that
 * the SDP is conveyed over a secure channel.  The encryption key
 * is interpreted as text according to the charset attribute; use
 * the "k=base64:" method to convey characters that are otherwise
 * prohibited in SDP.
 */
final case class ClearEncryptionKey(key: String) extends EncryptionKey

/**
 * {{{k=base64:<encoded encryption key>}}}
 * The encryption key is included in this key field but has been
 * base64 encoded [12] because it includes characters that are
 * prohibited in SDP.  This method MUST NOT be used unless it can
 * be guaranteed that the SDP is conveyed over a secure channel.
 */
final case class Base64EncryptionKey(key: Array[Byte]) extends EncryptionKey

/**
 * A Uniform Resource Identifier is included in the key field.
 * The URI refers to the data containing the key, and may require
 * additional authentication before the key can be returned.  When
 * a request is made to the given URI, the reply should specify
 * the encoding for the key.  The URI is often an Secure Socket
 * Layer/Transport Layer Security (SSL/TLS)-protected HTTP URI
 * ("https:"), although this is not required.
 * {{{k=uri:<URI to obtain key>}}}
 */
final case class UriEncryptionKey(keyUri: String) extends EncryptionKey

/**
 * {{{k=prompt}}}
 *
 * No key is included in this SDP description, but the session or
 * media stream referred to by this key field is encrypted.  The
 * user should be prompted for the key when attempting to join the
 * session, and this user-supplied key should then be used to
 *
 * decrypt the media streams.  The use of user-specified keys is
 * NOT RECOMMENDED, since such keys tend to have weak security
 * properties.
 */
case object PromptEncryptionKey extends EncryptionKey

sealed trait Attribute { def key: String }

final case class PropertyAttribute(key: String) extends Attribute
final case class ValueAttribute(key: String, value: String) extends Attribute

/**
 * {{{c=<nettype> <addrtype> <connection-address>}}}
 * The "c=" field contains connection data.
 *
 * A session description MUST contain either at least one "c=" field in
 * each media description or a single "c=" field at the session level.
 * It MAY contain a single session-level "c=" field and additional "c="
 * field(s) per media description, in which case the per-media values
 * override the session-level settings for the respective media.
 *
 * The first sub-field ("<nettype>") is the network type, which is a
 * text string giving the type of network.  Initially, "IN" is defined
 * to have the meaning "Internet", but other values MAY be registered in
 * the future (see Section 8).
 *
 * The second sub-field ("<addrtype>") is the address type.  This allows
 * SDP to be used for sessions that are not IP based.  This memo only
 * defines IP4 and IP6, but other values MAY be registered in the future
 * (see Section 8).
 *
 * The third sub-field ("<connection-address>") is the connection
 * address.  OPTIONAL sub-fields MAY be added after the connection
 * address depending on the value of the <addrtype> field.
 *
 * When the <addrtype> is IP4 and IP6, the connection address is defined
 * as follows:
 *
 * o  If the session is multicast, the connection address will be an IP
 * multicast group address.  If the session is not multicast, then
 * the connection address contains the unicast IP address of the
 * expected data source or data relay or data sink as determined by
 * additional attribute fields.  It is not expected that unicast
 * addresses will be given in a session description that is
 * communicated by a multicast announcement, though this is not
 * prohibited.
 *
 * o  Sessions using an IPv4 multicast connection address MUST also have
 * a time to live (TTL) value present in addition to the multicast
 * address.  The TTL and the address together define the scope with
 * which multicast packets sent in this conference will be sent.  TTL
 * values MUST be in the range 0-255.  Although the TTL MUST be
 * specified, its use to scope multicast traffic is deprecated;
 * applications SHOULD use an administratively scoped address
 * instead.
 * The TTL for the session is appended to the address using a slash as a
 * separator.  An example is:
 *
 * {{{c=IN IP4 224.2.36.42/127}}}
 *
 * IPv6 multicast does not use TTL scoping, and hence the TTL value MUST
 * NOT be present for IPv6 multicast.  It is expected that IPv6 scoped
 * addresses will be used to limit the scope of conferences.
 *
 * Hierarchical or layered encoding schemes are data streams where the
 * encoding from a single media source is split into a number of layers.
 * The receiver can choose the desired quality (and hence bandwidth) by
 * only subscribing to a subset of these layers.  Such layered encodings
 * are normally transmitted in multiple multicast groups to allow
 * multicast pruning.  This technique keeps unwanted traffic from sites
 * only requiring certain levels of the hierarchy.  For applications
 * requiring multiple multicast groups, we allow the following notation
 * to be used for the connection address:
 *
 * {{{<base multicast address>[/<ttl>]/<number of addresses>}}}
 *
 * If the number of addresses is not given, it is assumed to be one.
 * Multicast addresses so assigned are contiguously allocated above the
 * base address, so that, for example:
 *
 * {{{c=IN IP4 224.2.1.1/127/3}}}
 *
 * would state that addresses 224.2.1.1, 224.2.1.2, and 224.2.1.3 are to
 * be used at a TTL of 127.  This is semantically identical to including
 * multiple "c=" lines in a media description:
 *
 */
final case class ConnectionData(
  nettype: NetworkType,
  addrtype: AddressType,
  `connection-address`: InetSocketAddress)


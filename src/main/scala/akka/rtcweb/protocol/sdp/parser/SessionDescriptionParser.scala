package akka.rtcweb.protocol.sdp.parser

import java.net.{ InetSocketAddress }

import akka.parboiled2._
import akka.rtcweb.protocol.sdp._
import akka.rtcweb.protocol.sdp.parser.CharacterClasses._
import akka.shapeless.{ HNil, :: }
import scala.collection.immutable.Seq

import scala.util.{ Try, Failure, Success }

/** Plain SDP parser **/
private[sdp] class PlainSessionDescriptionParserImpl(val input: ParserInput) extends Parser with CommonRules with Base64Parsing with NoSessionAttributeExtension with NoMediaAttributeExtension
    with StringBuilding
    with SessionDescriptionParser with CommonSdpParser with MediaParser {

  def parseSessionDescription(): Try[SessionDescription] =
    `session-description`.run()

}

object SessionDescriptionParser {

  def parse(payload: String): SessionDescription = new PlainSessionDescriptionParserImpl(ParserInput(payload)).parseSessionDescription().get
}

private[protocol] trait SessionAttributeExtensionRule {
  def sessionAttributesExtensionsRule: Rule1[ExtensionAttribute]
}

trait NoSessionAttributeExtension extends SessionAttributeExtensionRule {
  this: Parser =>
  override def sessionAttributesExtensionsRule: Rule1[ExtensionAttribute] = MISMATCH
}

private[protocol] trait MediaAttributeExtensionRule {
  def mediaAttributesExtensionsRule: Rule1[ExtensionAttribute]
}

trait NoMediaAttributeExtension extends MediaAttributeExtensionRule {
  this: Parser =>
  override def mediaAttributesExtensionsRule: Rule1[ExtensionAttribute] = MISMATCH
}

private[protocol] trait CommonSdpParser {
  this: Parser with CommonRules with Base64Parsing ⇒

  /** information-field =   [%x69 "=" text CRLF] */
  def `information-field`: Rule1[String] = rule {
    str("i=") ~ text ~ CRLF
  }

  /** key-field = [%x6b "=" key-type CRLF] */
  def `key-field`: Rule1[EncryptionKey] = rule {
    str("k=") ~ `key-type` ~ CRLF
  }

  /**
   * {{{key-type = %x70 %x72 %x6f %x6d %x70 %x74 /     ; "prompt"
   * %x63 %x6c %x65 %x61 %x72 ":" text           / ; "clear:"
   * %x62 %x61 %x73 %x65 "64:" base64            /  ; "base64:"
   * %x75 %x72 %x69 ":" uri              ; "uri:"}}}
   */
  def `key-type`: Rule1[EncryptionKey] = rule {
    (str("prompt") ~ push(PromptEncryptionKey)) |
      (str("clear:") ~ `byte-string` ~> ((s) => ClearEncryptionKey(s))) |
      (str("base64:") ~ rfc2045String ~> ((bytes) => Base64EncryptionKey(bytes))) |
      (str("uri:") ~ `non-ws-string` ~> ((s) => UriEncryptionKey(s)))
  }

  /**
   * attribute =  (att-field ":" att-value) / att-field
   * att-field =           token
   * att-value =           byte-string
   */
  def attribute: Rule1[Attribute] = rule {
    (token ~ ch(':') ~ `byte-string` ~> ((t, v) => ValueAttribute(t, v))) |
      (token ~> ((t: String) => PropertyAttribute(t)))
  }

  /**connection-field =    [%x63 "=" nettype SP addrtype SP connection-address CRLF] */
  def `connection-field`: Rule1[ConnectionData] = rule {
    str("c=") ~ nettype ~ SP ~ addrtype ~ SP ~ `connection-address` ~ CRLF ~> ((a, b, c) ⇒ ConnectionData(a, b, c))
  }

  def nettype: Rule1[NetworkType] = rule {
    str("IN") ~ push(NetworkType.IN)
  }

  def addrtype: Rule1[AddressType] = rule {
    str("IP4") ~ push(AddressType.IP4) | str("IP6") ~ push(AddressType.IP6)
  }

  /** connection-address =  multicast-address / unicast-address */
  //simplified: all adresses are treated as unicast addresses
  def `connection-address`: Rule1[InetSocketAddress] = rule { `unicast-address` }

  /** unicast-address =     IP4-address / IP6-address / FQDN / extn-addr */
  // simplified
  def `unicast-address`: Rule1[InetSocketAddress] = rule {
    text ~> { (a: String) => InetSocketAddress.createUnresolved(a, 0) }
  }

  def `connection-address SP port`: Rule1[InetSocketAddress] = rule { text ~ SP ~ port ~> ((a: String, p: Int) => InetSocketAddress.createUnresolved(a, p)) }

  def port: Rule1[Int] = rule { number ~> ((l: Long) => l.toInt) }

  def `bandwidth-field`: Rule1[BandwidthInformation] = rule {
    str("b=") ~ `bandwidth-type` ~ ch(':') ~ number ~ CRLF ~> ((t: BandwidthType, n: Long) ⇒ BandwidthInformation(t, n.toInt))
  }

  def `bandwidth-type`: Rule1[BandwidthType] = rule {
    (str("CT") ~ push(BandwidthType.CT)) |
      (str("AS") ~ push(BandwidthType.AS)) |
      (str("RS") ~ push(BandwidthType.RS)) |
      (str("RR") ~ push(BandwidthType.RR)) |
      (capture(str("X-") ~ oneOrMore(ALPHANUM)) ~> (s ⇒ BandwidthType.Experimental(s)))
  }

}

private[protocol] trait MediaParser {
  this: Parser with CommonSdpParser with CommonRules with Base64Parsing with StringBuilding with MediaAttributeExtensionRule ⇒

  /**
   * media-descriptions =
   * media-field
   * information-field
   * connection-field
   * bandwidth-fields
   * key-field
   * attribute-fields
   */
  def `media-description` = rule {
    `media-field` ~
      optional(`information-field`) ~
      zeroOrMore(`connection-field`) ~
      zeroOrMore(`bandwidth-field`) ~
      optional(`key-field`) ~ // the ignored zeroOrMore(`bandwidth-field`) is a workaround for chromes missplaced sdp bandwidth rendering
      zeroOrMore(`media-attribute-field` ~ zeroOrMore(`bandwidth-field`) ~> ((attr, ignore) => attr)) ~> ((mf, i, conn, bw, key, attr) => MediaDescription(mf._1, i, mf._2, mf._3, attr, mf._4, conn, bw, key))
  }

  /** media-field = %x6d "=" media SP port ["/" integer] SP proto 1*(SP fmt) CRLF */
  def `media-field`: Rule1[(Media, PortRange, MediaTransportProtocol, Seq[String])] = rule {
    str("m=") ~ media ~ SP ~ port ~ optional(ch('/') ~ integer) ~ SP ~ proto ~ zeroOrMore(SP ~ fmt) ~ CRLF ~>
      ((m: Media, port: Int, portRange: Option[Long], proto: MediaTransportProtocol, fmts: Seq[String]) => (m, PortRange(port, portRange.map(_.toInt)), proto, fmts))
  }

  /** fmt = token ; typically an RTP payload type for audio and video media */
  def fmt = rule {
    token
  }

  /** media = token  ;typically "audio", "video", "text", "application" */
  def media: Rule1[Media] = rule {
    str("audio") ~ push(Media.audio) |
      str("video") ~ push(Media.video) |
      str("text") ~ push(Media.text) |
      str("application") ~ push(Media.application) |
      token ~> ((t) => CustomMedia(t))
  }

  /** proto = token *("/" token) ;typically "RTP/AVP" or "udp" */
  // simplified
  def proto: Rule1[MediaTransportProtocol] = rule {
    str("udp") ~ push(MediaTransportProtocol.udp) |
      str("RTP/AVP") ~ push(MediaTransportProtocol.`RTP/AVP`) |
      str("RTP/SAVPF") ~ push(MediaTransportProtocol.`RTP/SAVPF`) |
      str("RTP/SAVP") ~ push(MediaTransportProtocol.`RTP/SAVP`)
  }

  /** attribute-fields =    *(%x61 "=" attribute CRLF) */
  def `media-attribute-field`: Rule1[Attribute] = rule {
    str("a=") ~ (mediaAttributesExtensionsRule | attribute) ~ CRLF
  }

}

/**
 * ; SDP Syntax
 * session-description =
 *   proto-version
 *   origin-field
 *   session-name-field
 *   information-field
 *   uri-field
 *   email-fields
 *   phone-fields
 *   connection-field
 *   bandwidth-fields
 *   time-fields
 *   key-field
 *   attribute-fields
 *   media-descriptions
 */
private[protocol] trait SessionDescriptionParser {
  this: Parser with CommonSdpParser with MediaParser with CommonRules with Base64Parsing with StringBuilding with SessionAttributeExtensionRule with MediaAttributeExtensionRule ⇒

  import CharacterClasses._

  def `session-description` = rule {
    part1 ~ part2 ~ zeroOrMore(`media-description`) ~ EOI ~>
      ((p1, p2, mds) ⇒ SessionDescription(p1._1, p1._2, p1._3, p1._4, p1._5, p2._1, p2._2, p2._3, p2._4, p2._5, p2._6, p2._7, mds))
  }

  /** proto-version =       %x76 "=" 1*DIGIT CRLF */
  def `proto-version`: Rule1[ProtocolVersion] = rule {
    str("v=0") ~ push(ProtocolVersion.`0`) ~ CRLF
  }

  /** origin-field =        %x6f "=" username SP sess-id SP sess-version SP nettype SP addrtype SP unicast-address CRLF */
  def `origin-field`: Rule1[Origin] = rule {
    str("o=") ~ (username ~ SP ~ `sess-id` ~ SP ~ `sess-version` ~ SP ~ nettype ~ SP ~ addrtype ~ SP ~ `unicast-address`) ~ CRLF ~> ((a, b, c, d, e, f) ⇒ Origin(a, b, c, d, e, f))
  }

  /** username =            non-ws-string */
  def username: Rule1[Option[String]] = rule {
    (ch('-') ~ push(Option.empty[String])) | (`non-ws-string` ~> (s ⇒ Option[String](s)))
  }

  /** sess-id =        1*DIGIT */
  def `sess-id` = `numeric-string`

  /** sess-version =        1*DIGIT */
  def `sess-version` = number

  /** session-name-field =  %x73 "=" text CRLF */
  def `session-name-field`: Rule1[Option[String]] = rule {
    str("s=") ~ ((SP ~ push(None)) | (text ~> (s ⇒ Some(s)))) ~ CRLF
  }

  /** uri-field =           [%x75 "=" uri CRLF] */
  def `uri-field`: Rule1[String] = rule {
    str("u=") ~ `non-ws-string` ~ CRLF
  }

  /** email-fields =        *(%x65 "=" email-address CRLF) */
  //TODO: simplified
  def `email-field`: Rule1[String] = rule {
    str("e=") ~ text ~ CRLF

  }

  /** %x70 "=" phone-number CRLF */
  //TODO: simplified
  def `phone-field`: Rule1[String] = rule {
    str("p=") ~ text ~ CRLF
  }

  /**  time-fields =         1*( %x74 "=" start-time SP stop-time (CRLF repeat-fields) CRLF) [zone-adjustments CRLF] **/
  def `time-field`: Rule1[Timing] = rule {
    str("t=") ~ numberDifferentThanZero ~ SP ~ numberDifferentThanZero ~ CRLF ~ optional(`repeat-field`) ~ optional(`zone-adjustments`) ~>
      ((a: Option[Long], b: Option[Long], r: Option[RepeatTimes], adjustments: Option[Seq[TimeZoneAdjustment]]) ⇒ Timing(a, b, r, adjustments.getOrElse(Nil)))
  }

  /** repeat-fields =       %x72 "=" repeat-interval SP typed-time 1*(SP typed-time) */
  def `repeat-field`: Rule1[RepeatTimes] = rule {
    str("r=") ~ `repeat-interval` ~ SP ~ `typed-time` ~ oneOrMore(SP ~ `typed-time`) ~ CRLF ~> ((a, b, c) ⇒ RepeatTimes(a, b, c))
  }

  /** repeat-interval =     POS-DIGIT *DIGIT [fixed-len-time-unit] */
  def `repeat-interval`: Rule1[TimeSpan] = rule {
    integer ~ optional(`fixed-len-time-unit`) ~> ((a: Long, b: Option[TimeUnit]) ⇒ TimeSpan(a, b.getOrElse(TimeUnit.Seconds)))
  }

  /** zone-adjustments =  %x7a "=" time SP ["-"] typed-time (SP time SP ["-"] typed-time) */
  // todo: simplified, no negative allowed
  def `zone-adjustments`: Rule1[Seq[TimeZoneAdjustment]] = rule {
    str("z=") ~ `zone-adjustment` ~ zeroOrMore(SP ~ `zone-adjustment`) ~ CRLF ~>
      ((first: TimeZoneAdjustment, more: Seq[TimeZoneAdjustment]) => Seq(first) ++ more)
  }

  def `zone-adjustment`: Rule1[TimeZoneAdjustment] = rule {
    time ~ SP ~ `typed-time` ~> ((a: Long, b: TimeSpan) => TimeZoneAdjustment(a, b))
  }

  def time = number

  /** typed-time =          1*DIGIT [fixed-len-time-unit] */
  def `typed-time`: Rule1[TimeSpan] = rule {
    number ~ optional(`fixed-len-time-unit`) ~> ((a: Long, b: Option[TimeUnit]) ⇒ TimeSpan(a, b.getOrElse(TimeUnit.Seconds)))
  }

  /** fixed-len-time-unit = %x64 / %x68 / %x6d / %x73 */
  def `fixed-len-time-unit`: Rule1[TimeUnit] = rule {
    (ch('d') ~ push(TimeUnit.Days)) |
      (ch('h') ~ push(TimeUnit.Hours)) |
      (ch('m') ~ push(TimeUnit.Minutes)) |
      (ch('s') ~ push(TimeUnit.Seconds))
  }

  /** attribute-fields =    *(%x61 "=" attribute CRLF) */
  def `session-attribute-field`: Rule1[Attribute] = rule {
    str("a=") ~ (sessionAttributesExtensionsRule | attribute) ~ CRLF
  }

  private def part1 = rule {
    (`proto-version` ~
      `origin-field` ~
      `session-name-field` ~
      optional(`information-field`) ~
      optional(`uri-field`)) ~> ((v, o, s, i, u) ⇒ (v, o, s, i, u))
  }

  private def part2 = rule {
    (zeroOrMore(`email-field`) ~
      zeroOrMore(`phone-field`) ~
      optional(`connection-field`) ~
      optional(`bandwidth-field`) ~
      zeroOrMore(`time-field`) ~

      //todo: zone corrections
      optional(`key-field`) ~
      zeroOrMore(`session-attribute-field`)) ~> ((e, p, c, b, t, k, a) ⇒ (e, p, c, b, t, k, a))
  }

}

/**
 * {{{
 *
 * 9.  SDP Grammar
 *
 * This section provides an Augmented BNF grammar for SDP.  ABNF is
 * defined in [4].
 *
 * *
 * ; sub-rules of 'o='
 * username =            non-ws-string
 * ;pretty wide definition, but doesn't
 * ;include space
 *
 * sess-id =             1*DIGIT
 * ;should be unique for this username/host
 *
 * sess-version =        1*DIGIT
 *
 * nettype =             token
 * ;typically "IN"
 *
 * addrtype =            token
 * ;typically "IP4" or "IP6"
 *
 * ; sub-rules of 'u='
 * uri =                 URI-reference
 * ; see RFC 3986
 *
 *
 * ; sub-rules of 'e=', see RFC 2822 for definitions
 * email-address        = address-and-comment / dispname-and-address
 * / addr-spec
 * address-and-comment  = addr-spec 1*SP "(" 1*email-safe ")"
 * dispname-and-address = 1*email-safe 1*SP "<" addr-spec ">"
 *
 * ; sub-rules of 'p='
 * phone-number =        phone *SP "(" 1*email-safe ")" /
 * 1*email-safe "<" phone ">" /
 * phone
 *
 * phone =               ["+"] DIGIT 1*(SP / "-" / DIGIT)
 *
 * ; sub-rules of 'c='
 * connection-address =  multicast-address / unicast-address
 *
 * ; sub-rules of 'b='
 * bwtype =              token
 *
 * bandwidth =           1*DIGIT
 *
 * ; sub-rules of 't='
 * start-time =          time / "0"
 *
 * stop-time =           time / "0"
 *
 * time =                POS-DIGIT 9*DIGIT
 * ; Decimal representation of NTP time in
 * ; seconds since 1900.  The representation
 * ; of NTP time is an unbounded length field
 * ; containing at least 10 digits.  Unlike the
 * ; 64-bit representation used elsewhere, time
 * ; in SDP does not wrap in the year 2036.
 *
 * ; sub-rules of 'r=' and 'z='
 * repeat-interval =     POS-DIGIT *DIGIT [fixed-len-time-unit]
 *
 * typed-time =          1*DIGIT [fixed-len-time-unit]
 *
 * fixed-len-time-unit = %x64 / %x68 / %x6d / %x73
 *
 * ; sub-rules of 'k='
 * key-type =            %x70 %x72 %x6f %x6d %x70 %x74 /     ; "prompt"
 * %x63 %x6c %x65 %x61 %x72 ":" text / ; "clear:"
 * %x62 %x61 %x73 %x65 "64:" base64 /  ; "base64:"
 * %x75 %x72 %x69 ":" uri              ; "uri:"
 *
 * base64      =         *base64-unit [base64-pad]
 *
 *
 *
 * Handley, et al.             Standards Track                    [Page 41]
 *
 * RFC 4566                          SDP                          July 2006
 *
 *
 * base64-unit =         4base64-char
 * base64-pad  =         2base64-char "==" / 3base64-char "="
 * base64-char =         ALPHA / DIGIT / "+" / "/"
 *
 * ; sub-rules of 'a='
 * attribute =           (att-field ":" att-value) / att-field
 *
 * att-field =           token
 *
 * att-value =           byte-string
 *
 * ; sub-rules of 'm='
 * media =               token
 * ;typically "audio", "video", "text", or
 * ;"application"
 *
 * fmt =                 token
 * ;typically an RTP payload type for audio
 * ;and video media
 *
 * proto  =              token *("/" token)
 * ;typically "RTP/AVP" or "udp"
 *
 * port =                1*DIGIT
 *
 * ; generic sub-rules: addressing
 * unicast-address =     IP4-address / IP6-address / FQDN / extn-addr
 *
 * multicast-address =   IP4-multicast / IP6-multicast / FQDN
 * / extn-addr
 *
 * IP4-multicast =       m1 3( "." decimal-uchar )
 * "/" ttl [ "/" integer ]
 * ; IPv4 multicast addresses may be in the
 * ; range 224.0.0.0 to 239.255.255.255
 *
 * m1 =                  ("22" ("4"/"5"/"6"/"7"/"8"/"9")) /
 * ("23" DIGIT )
 *
 * IP6-multicast =       hexpart [ "/" integer ]
 * ; IPv6 address starting with FF
 *
 * ttl =                 (POS-DIGIT *2DIGIT) / "0"
 *
 * FQDN =                4*(alpha-numeric / "-" / ".")
 * ; fully qualified domain name as specified
 * ; in RFC 1035 (and updates)
 *
 *
 *
 * IP4-address =         b1 3("." decimal-uchar)
 *
 * b1 =                  decimal-uchar
 * ; less than "224"
 *
 * ; The following is consistent with RFC 2373 [30], Appendix B.
 * IP6-address =         hexpart [ ":" IP4-address ]
 *
 * hexpart =             hexseq / hexseq "::" [ hexseq ] /
 * "::" [ hexseq ]
 *
 * hexseq  =             hex4 *( ":" hex4)
 *
 * hex4    =             1*4HEXDIG
 *
 * ; Generic for other address families
 * extn-addr =           non-ws-string
 *
 * ; generic sub-rules: datatypes
 * text =                byte-string
 * ;default is to interpret this as UTF8 text.
 * ;ISO 8859-1 requires "a=charset:ISO-8859-1"
 * ;session-level attribute to be used
 *
 * byte-string =         1*(%x01-09/%x0B-0C/%x0E-FF)
 * ;any byte except NUL, CR, or LF
 *
 * non-ws-string =       1*(VCHAR/%x80-FF)
 * ;string of visible characters
 *
 * token-char =          %x21 / %x23-27 / %x2A-2B / %x2D-2E / %x30-39
 * / %x41-5A / %x5E-7E
 *
 * token =               1*(token-char)
 *
 * email-safe =          %x01-09/%x0B-0C/%x0E-27/%x2A-3B/%x3D/%x3F-FF
 * ;any byte except NUL, CR, LF, or the quoting
 * ;characters ()<>
 *
 * integer =             POS-DIGIT *DIGIT
 *
 * ; generic sub-rules: primitives
 * alpha-numeric =       ALPHA / DIGIT
 *
 * POS-DIGIT =           %x31-39 ; 1 - 9
 *
 *
 *
 *
 *
 *
 * Handley, et al.             Standards Track                    [Page 43]
 *
 * RFC 4566                          SDP                          July 2006
 *
 *
 * decimal-uchar =       DIGIT
 * / POS-DIGIT DIGIT
 * / ("1" 2*(DIGIT))
 * / ("2" ("0"/"1"/"2"/"3"/"4") DIGIT)
 * / ("2" "5" ("0"/"1"/"2"/"3"/"4"/"5"))
 * }}}
 */

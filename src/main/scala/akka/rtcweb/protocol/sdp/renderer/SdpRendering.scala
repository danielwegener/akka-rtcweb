package akka.rtcweb.protocol.sdp.renderer

import java.net.InetSocketAddress

import akka.parboiled2.util.Base64
import akka.rtcweb.protocol.sdp.MediaTransportProtocol._
import akka.rtcweb.protocol.sdp._
import akka.util.ByteString

trait SdpRendering {

  import Renderer._
  import Rendering._

  implicit val optionRenderer = Renderer.optionRenderer[String, String]("-")

  implicit val protocolVersionRenderer = new Renderer[ProtocolVersion] {
    override def render[R <: Rendering](r: R, value: ProtocolVersion): r.type =
      r ~ s"v=${value.value}" ~ CRLF
  }

  implicit val mediaRenderer = new Renderer[Media] {
    override def render[R <: Rendering](r: R, value: Media): r.type = r ~ (value match {
      case Media.application => "application"
      case Media.audio => "audio"
      case Media.video => "video"
      case Media.text => "text"
      case Media.message => "message"
      case CustomMedia(name) => name
    })
  }

  implicit val portRangeRenderer = new Renderer[PortRange] {
    override def render[R <: Rendering](r: R, value: PortRange): r.type = r ~ (value match {
      case PortRange(port, Some(range)) => s"$port/$range"
      case PortRange(port, None) => s"$port"
    })
  }

  implicit val mtpRenderer = Renderer.stringRenderer[MediaTransportProtocol] {
    case MediaTransportProtocol.udp => "udp"
    case `RTP/AVP` => "RTP/AVP"
    case `RTP/SAVP` => "RTP/SAVP"
    case `RTP/SAVPF` => "RTP/SAVPF"
    case `UDP/TLS/RTP/SAVP` => "UDP/TLS/RTP/SAVP"
    case `UDP/TLS/RTP/SAVPF` => "UDP/TLS/RTP/SAVPF"
  }

  implicit val nettypeRenderer = Renderer.stringRenderer[NetworkType] {
    case NetworkType.IN => "IN"
  }

  implicit val addressTypeRenderer = Renderer.stringRenderer[AddressType] {
    case AddressType.IP4 => "IP4"
    case AddressType.IP6 => "IP6"
  }

  implicit val inetSocketAddressRenderer = Renderer.stringRenderer[InetSocketAddress](_.getHostName)

  implicit val encryptionKeyRenderer = Renderer.stringRenderer[EncryptionKey] {
    case ClearEncryptionKey(key) => s"k=clear:$key"
    case Base64EncryptionKey(bytes) => "k=base64:" + Base64.rfc2045().encodeToString(bytes, false)
    case UriEncryptionKey(uri) => s"k=uri:$uri"
    case PromptEncryptionKey => "k=prompt"
  }

  implicit val connectionDataRenderer: Renderer[ConnectionData] = new Renderer[ConnectionData] {
    override def render[R <: Rendering](r: R, value: ConnectionData): r.type = value match {
      case ConnectionData(networkType, addrType, connectionAddress) =>
        r ~ "c=" ~ networkType ~ SP ~ addrType ~ SP ~ connectionAddress ~ CRLF
    }
  }

  implicit val bandwidthTypeRenderer = stringRenderer[BandwidthType] {
    case BandwidthType.AS => "AS"
    case BandwidthType.CT => "CT"
    case BandwidthType.RS => "RS"
    case BandwidthType.RR => "RR"
    case BandwidthType.Experimental(name) => name
  }

  implicit val bandwidthInformationRenderer = new Renderer[BandwidthInformation] {
    override def render[R <: Rendering](r: R, value: BandwidthInformation): r.type = value match {
      case BandwidthInformation(bwtype, bw) =>
        r ~ "b=" ~ bwtype ~ ':' ~ bw ~ CRLF
    }
  }
  implicit val originRenderer = originRendererMaker
  implicit val repeatTimesRenderer: Renderer[RepeatTimes] = new Renderer[RepeatTimes] {
    override def render[R <: Rendering](r: R, value: RepeatTimes): r.type =
      r ~ "r=" ~ /* FIXME */ CRLF
  }
  implicit val timeZoneAdjustmentRenderer: Renderer[TimeZoneAdjustment] = new Renderer[TimeZoneAdjustment] {
    override def render[R <: Rendering](r: R, value: TimeZoneAdjustment): r.type =
      r ~ "t=" ~ /* FIXME */ CRLF
  }
  implicit val timingRenderer = new Renderer[Timing] {
    override def render[R <: Rendering](r: R, value: Timing): r.type = value match {
      case (Timing(startTime, stopTime, repeatings, zoneAdjustments)) =>
        r ~ "t=" ~ startTime.getOrElse(0L) ~ SP ~ stopTime.getOrElse(0L) ~ CRLF
        if (repeatings.isDefined) r ~ repeatings.get
        zoneAdjustments.foreach(r ~ _)
        r
    }
  }
  implicit val attributeRenderer = new Renderer[Attribute] {
    override def render[R <: Rendering](r: R, renderee: Attribute): r.type = renderee match {
      case PropertyAttribute(key) => r ~ s"a=$key" ~ CRLF
      case ValueAttribute(key, value) => r ~ s"a=$key:$value" ~ CRLF
      case ea: ExtensionAttribute => r ~ "a="; renderAttributeExtensions(r, ea) ~ CRLF
    }
  }
  implicit val mediaDescriptionRenderer = makeMediaDescriptionRenderer
  implicit val sessionDescriptionRenderer = new Renderer[SessionDescription] {

    override def render[R <: Rendering](r: R, s: SessionDescription): r.type = {
      r ~ s.protocolVersion ~ s.origin
      r ~ "s=" ~ s.sessionName ~ CRLF
      s.sessionInformation.foreach(r ~ "i=" ~ _ ~ CRLF)
      s.descriptionUri.foreach(r ~ "u=" ~ _ ~ CRLF)
      s.emailAddresses.foreach(r ~ "e=" ~ _ ~ CRLF)
      s.phoneNumbers.foreach(r ~ "p=" ~ _ ~ CRLF)
      s.connectionInformation.foreach(r ~ _)
      s.bandwidthInformation.foreach(r ~ _)
      s.timings.foreach(r ~ _)
      s.encryptionKey.foreach(r ~ _ ~ CRLF)
      s.sessionAttributes.foreach(r ~ _)
      s.mediaDescriptions.foreach(r ~ _)
      r
    }
  }

  def renderAttributeExtensions[R <: Rendering](r: R, renderee: ExtensionAttribute): r.type

  private def originRendererMaker(implicit nettypeRenderer: Renderer[NetworkType]): Renderer[Origin] = {
    new Renderer[Origin] {
      override def render[R <: Rendering](r: R, o: Origin): r.type = {
        r ~ "o=" ~ o.username ~ SP
        r ~ o.`sess-id` ~ SP ~ o.`sess-version` ~ SP ~ o.nettype ~ SP ~ o.addrtype ~ SP ~ o.`unicast-address` ~ CRLF
      }
    }
  }

  private def makeMediaDescriptionRenderer(implicit mediaRenderer: Renderer[Media],
    portRangeRenderer: Renderer[PortRange],
    mtpRenderer: Renderer[MediaTransportProtocol],
    connectionDataRenderer: Renderer[ConnectionData],
    encryptionKeyRenderer: Renderer[EncryptionKey]): Renderer[MediaDescription] = new Renderer[MediaDescription] {
    override def render[R <: Rendering](r: R, value: MediaDescription): r.type = value match {
      case MediaDescription(media, mediaTitle, portRange, protocol, mediaAttributes, fmts, connectionInformation, bandwidthInformation, encryptionKey) =>
        r ~ "m=" ~ media ~ SP ~ portRange ~ SP ~ protocol
        fmts.foreach(r ~ SP ~ _)
        r ~ CRLF
        mediaTitle.foreach(r ~ "i=" ~ _ ~ CRLF)
        connectionInformation.foreach(r ~ _)
        bandwidthInformation.foreach(r ~ _)
        mediaAttributes.foreach(r ~ _)
        r
    }
  }

}

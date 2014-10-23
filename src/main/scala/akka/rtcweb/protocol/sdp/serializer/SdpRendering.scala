package akka.rtcweb.protocol.sdp.serializer

import java.net.InetSocketAddress

import akka.parboiled2.util.Base64
import akka.rtcweb.protocol.sdp._

object SdpRendering extends SdpRenderingLowPriorityImplicits {

  def render(ctx: StringRenderingContext, sessionDescription: SessionDescription): Unit = sessionSerializer.append(ctx, sessionDescription)

  def render(sessionDescription:SessionDescription):String = {
    val ctx = new StringRenderingContext()
    render(ctx, sessionDescription)
    ctx.result()
  }

}

trait SdpRenderingLowPriorityImplicits {

  implicit def mediaDescriptionSerializer(implicit mediaSerializer: StringRenderable[Media],
    portRangeSerializer: StringRenderable[PortRange],
    mtpSerializer: StringRenderable[MediaTransportProtocol],
    connectionDataSerializer: StringRenderable[ConnectionData],
    encryptionKeySerializer: StringRenderable[EncryptionKey]): StringRenderable[MediaDescription] = SimpleStringRenderable[MediaDescription] {
    case MediaDescription(media, mediaTitle, portRange, protocol, mediaAttributes, fmts, connectionInformation, encryptionKey) =>
      ???

  }

  implicit val protocolVersionSerializer: StringRenderable[ProtocolVersion] = SimpleStringRenderable(t => s"v=${t.value}")
  implicit val mediaSerializer: StringRenderable[Media] = SimpleStringRenderable {
    case Media.application => "application"
    case Media.audio => "audio"
    case Media.video => "video"
    case Media.text => "text"
    case Media.message => "message"
    case CustomMedia(name) => name
  }

  implicit val portRangeSerializer = SimpleStringRenderable[PortRange] {
    case PortRange(port, Some(range)) => s"$port/$range"
    case PortRange(port, None) => s"$port"
  }

  implicit val mtpSerializer = SimpleStringRenderable[MediaTransportProtocol] {
    case MediaTransportProtocol.udp => "udp"
    case MediaTransportProtocol.`RTP/AVP` => "RTP/AVP"
    case MediaTransportProtocol.`RTP/SAVP` => "RTP/SAVP"
    case MediaTransportProtocol.`RTP/SAVPF` => "RTP/SAVPF"
  }

  implicit def connectionDataSerializer(implicit nettypeSerializer: StringRenderable[NetworkType],
    addrtypeSerializer: StringRenderable[AddressType],
    connectionAddressSerializer: StringRenderable[InetSocketAddress]) = SimpleStringRenderable[ConnectionData] {
    case ConnectionData(networkType, addrType, connectionAddress) => ???
  }

  implicit val nettypeSerializer = SimpleStringRenderable[NetworkType] {
    case NetworkType.IN => "IN"
  }

  implicit val addressTypeSerializer = SimpleStringRenderable[AddressType] {
    case AddressType.IP4 => "IP4"
    case AddressType.IP6 => "IP6"
  }

  implicit val inetSocketAddressSerializer = SimpleStringRenderable[InetSocketAddress](_.getAddress.toString)

  implicit val encryptionKeySerializer = SimpleStringRenderable[EncryptionKey] {
    case ClearEncryptionKey(key) => s"k=clear:$key"
    case Base64EncryptionKey(bytes) => "k=base64:" + Base64.rfc2045().encodeToString(bytes, false)
    case UriEncryptionKey(uri) => s"k=uri:$uri"
    case PromptEncryptionKey => "k=prompt"
  }

  implicit val bandwidthInformationSerializer = SimpleStringRenderable[BandwidthInformation] {
    case BandwidthInformation(bwtype, bw) => "" + ???
  }

  implicit val originSerializer = originSerializerMaker

  private def originSerializerMaker(implicit nettypeSerializer: StringRenderable[NetworkType],
    addrtypeSerializer: StringRenderable[AddressType],
    connectionAddressSerializer: StringRenderable[InetSocketAddress]): StringRenderable[Origin] = {
    new StringRenderable[Origin] {
      override def append(context: StringRenderingContext, t: Origin): Unit = ???
    }
  }

  implicit val sessionSerializer = sessionSerializerMaker

  implicit val timingSerializer: StringRenderable[Timing] = SimpleStringRenderable {
    case Timing(startTime, stopTime, repeatings, zoneAdjustments) => "" + ???
  }

  implicit val attributeSerializer: StringRenderable[Attribute] = SimpleStringRenderable {
    case PropertyAttribute(key) => s"a=$key"
    case ValueAttribute(key, value) => s"a=$key:$value"
  }

  private def sessionSerializerMaker(
    implicit pvSerializer: StringRenderable[ProtocolVersion],
    originSerializer: StringRenderable[Origin],
    connectionDataSerializer: StringRenderable[ConnectionData],
    bandwidthInformationSerializer: StringRenderable[BandwidthInformation],
    timingSerializer: StringRenderable[Timing],
    encryptionKey: StringRenderable[EncryptionKey],
    sessionAttributesSerializer: StringRenderable[Attribute],
    mediaDescriptionSerializer: StringRenderable[MediaDescription]): StringRenderable[SessionDescription] = ???

}

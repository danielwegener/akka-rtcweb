package akka.rtcweb.protocol.dtls

import java.net.InetSocketAddress

import akka.actor.{ ExtendedActorSystem, ExtensionKey }
import akka.io.Inet
import akka.rtcweb.protocol.dtls.handshake.SessionId
import akka.rtcweb.protocol.dtls.record.DtlsPlaintext
import akka.stream.MaterializerSettings
import akka.stream.io2.StreamUdp.UdpPacket
import akka.util.ByteString

import com.typesafe.config.Config
import org.reactivestreams.{ Processor, Subscriber, Publisher }

import scala.collection.immutable

object StreamDtls extends ExtensionKey[StreamDtlsExt] {

  /**
   * The Bind message is send to the Dtls manager actor, which is obtained via
   * `IO(StreamDtls)`, in order to bind to a listening socket. The manager
   * replies with a [[StreamDtls.DtlsConnection]]. If the local port is set to 0 in
   * the Bind message, then the [[StreamDtls.DtlsConnection]] message should be inspected to find
   * the actual port which was bound to.
   *
   * @param localAddress the socket address to bind to; use port zero for automatic assignment (i.e. an ephemeral port)
   * @param options Please refer to [[akka.io.UdpSO]] for a list of all supported options.
   */
  final case class Bind(settings: MaterializerSettings, localAddress: InetSocketAddress,
    options: immutable.Traversable[Inet.SocketOption] = Nil,
    serverSettings: Option[ServerSettings] = None)

  /**
   * The UdpConnection message will be sent in response to the Bind message.
   * @param localAddress
   * @param inputStream
   * @param outputStream
   */
  case class DtlsConnection(localAddress: InetSocketAddress,
      remoteAddress: InetSocketAddress,
      inputStream: Publisher[ByteString],
      outputStream: Subscriber[ByteString]) {
    def handleWith(processor: Processor[ByteString, ByteString]): Unit = {
      processor.subscribe(outputStream)
      inputStream.subscribe(processor)
    }
  }

  case class DtlsRecord(content: DtlsPlaintext, remoteAddress: InetSocketAddress)

}

class StreamDtlsExt(system: ExtendedActorSystem) extends akka.io.IO.Extension {
  val Settings = new Settings(system.settings.config getConfig "akka.rtcweb.dtls")
  class Settings private[StreamDtlsExt] (config: Config) {
    val ManagerDispatcher = config getString "manager-dispatcher"
  }

  val manager = system.actorOf(props = StreamDtlsManager.props(Settings), name = "IO-DTLS")
}


/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.io

import java.net.{ InetSocketAddress, URLEncoder }

import akka.actor._
import akka.io.Inet.SocketOption
import akka.io.{ IO, Udp }
import akka.stream.MaterializerSettings
import akka.stream.impl.ActorProcessor
import akka.stream.io.StreamUdp.UdpPacket
import akka.stream.io.StreamUdpManager.ExposedProcessor
import akka.util.ByteString
import org.reactivestreams.{ Processor, Publisher, Subscriber }

import scala.collection._

object StreamUdp extends ExtensionId[StreamUdpExt] with ExtensionIdProvider {

  override def lookup = StreamUdp
  override def createExtension(system: ExtendedActorSystem): StreamUdpExt = new StreamUdpExt(system)
  override def get(system: ActorSystem): StreamUdpExt = super.get(system)

  /**
   * UpdPacket is the input/ouput message.  Data to be sent to the address or received
   * will be deliver with this message.
   * @param content
   * @param address
   */
  case class UdpPacket(content: ByteString, address: InetSocketAddress)

  /**
   * The UdpConnection message will be sent in response to the Bind message.
   * @param localAddress
   * @param inputStream
   * @param outputStream
   */
  case class UdpConnection(localAddress: InetSocketAddress,
      inputStream: Publisher[UdpPacket],
      outputStream: Subscriber[UdpPacket]) {
    def handleWith(processor: Processor[UdpPacket, UdpPacket]): Unit = {
      processor.subscribe(outputStream)
      inputStream.subscribe(processor)
    }
  }

  /**
   * The Bind message is send to the StreamUdp manager actor, which is obtained via
   * `IO(StreamUdp)`, in order to bind to a listening socket. The manager
   * replies with a [[StreamUdp.UdpConnection]]. If the local port is set to 0 in
   * the Bind message, then the [[StreamUdp.UdpConnection]] message should be inspected to find
   * the actual port which was bound to.
   *
   * @param localAddress the socket address to bind to; use port zero for automatic assignment (i.e. an ephemeral port)
   * @param options Please refer to [[akka.io.UdpSO]] for a list of all supported options.
   */
  case class Bind(settings: MaterializerSettings,
    localAddress: InetSocketAddress,
    options: immutable.Traversable[SocketOption] = Nil)

}

/**
 * INTERNAL API
 */
private[akka] class StreamUdpExt(system: ExtendedActorSystem) extends IO.Extension {
  val manager: ActorRef = system.systemActorOf(Props[StreamUdpManager], name = "IO-UDP-STREAM")
}

/**
 * INTERNAL API
 */
private[akka] object StreamUdpManager {
  private[akka] case class ExposedProcessor(processor: Processor[UdpPacket, UdpPacket])
}

/**
 * INTERNAL API
 */
private[akka] class StreamUdpManager extends Actor {

  var nameCounter = 0
  def encName(prefix: String, address: InetSocketAddress) = {
    nameCounter += 1
    s"$prefix-$nameCounter-${URLEncoder.encode(address.toString, "utf-8")}"
  }

  def receive: Receive = {
    case StreamUdp.Bind(settings, localAddress, options) ⇒
      val publisherActor = context.actorOf(UdpActor.props(
        Udp.Bind(context.system.deadLetters, localAddress, options),
        requester = sender(),
        settings), name = encName("udp-server", localAddress))
      publisherActor ! ExposedProcessor(ActorProcessor[UdpPacket, UdpPacket](publisherActor))
  }
}


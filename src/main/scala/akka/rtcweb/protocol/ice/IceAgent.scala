package akka.rtcweb.protocol.ice

import java.net.{InetAddress, NetworkInterface}

import akka.io.Udp
import akka.rtcweb.protocol.ice.stun.StunMessage
import akka.rtcweb.protocol.jsep.RTCPeerConnection.StunServerDescription
import scodec.bits.{ByteVector}

import scala.collection.JavaConversions._

import akka.actor.{Props, ActorLogging, Actor}

object IceAgent {

  def props(iceServers:List[StunServerDescription]) = Props(new IceAgent(iceServers))



}

class IceAgent private(iceServers:List[StunServerDescription]) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Udp.Bound(localWildcardAddress) if localWildcardAddress.getAddress.isAnyLocalAddress =>
      context.become(ready(localAddresses().toList, localWildcardAddress.getPort))
    case Udp.Bound(localAddress) => context.become(ready(List(localAddress.getAddress), localAddress.getPort))
  }

  def ready(localAddresses:List[InetAddress], port:Int): Receive = {
    case Udp.Received(data, sender) =>
      val decodec = StunMessage.codec.complete.decode(ByteVector.view(data.asByteBuffer).bits)
  }

  def localAddresses() = {
    val localInterfaces = NetworkInterface.getNetworkInterfaces.toSeq
    localInterfaces.flatMap(_.getInetAddresses.toSeq)
  }

}

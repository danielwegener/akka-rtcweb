package akka.rtcweb.protocol.ice

import java.net.{ InetSocketAddress, InetAddress, NetworkInterface }

import akka.io.Udp
import akka.rtcweb.protocol.ice.IceAgent.{ OnIceCandidate, GatherCandidates }
import akka.rtcweb.protocol.ice.stun.{ StunAttribute, StunMessage }
import akka.rtcweb.protocol.jsep.RTCPeerConnection.StunServerDescription
import akka.util.ByteString
import scodec.bits.{ BitVector, ByteVector }
import scala.collection.immutable.{ Queue, Seq }

import scala.collection.JavaConversions._

import akka.actor.{ ActorRef, Props, ActorLogging, Actor }

import scala.concurrent.forkjoin.ThreadLocalRandom

object IceAgent {

  def props(iceServers: Vector[StunServerDescription]) = Props(new IceAgent(iceServers))

  final case class OnIceCandidate(candidates: Seq[InetSocketAddress])
  object GatherCandidates

}

class IceAgent private (iceServers: Vector[StunServerDescription]) extends Actor with ActorLogging {

  require(!iceServers.isEmpty, "iceServers must not be empty")

  var outstandingTransactionIds: Vector[(BitVector, StunServerDescription)] = Vector.empty

  override def receive: Receive = {
    case Udp.Bound(localWildcardAddress) if localWildcardAddress.getAddress.isAnyLocalAddress =>
      context.become(ready(sender(), localAddresses(), localWildcardAddress.getPort))
    case Udp.Bound(localAddress) => context.become(ready(sender(), Vector(localAddress.getAddress), localAddress.getPort))
  }

  def ready(socket: ActorRef, localAddresses: Seq[InetAddress], port: Int): Receive = {
    case Udp.Received(data, sender) =>
      val decoded = StunMessage.codec.complete.decodeValue(ByteVector.view(data.asByteBuffer).bits)
      log.info(s"Received a decoded stun message: $decoded")
    case GatherCandidates =>
      sender() ! OnIceCandidate(localAddresses.map(address => new InetSocketAddress(address, port)))
      iceServers.foreach { server =>
        val stunBindingRequest = StunMessage(stun.Class.request, stun.Method.Binding, generateTransactionId())
        socket ! Udp.Send(ByteString.fromByteBuffer(StunMessage.codec.encodeValid(stunBindingRequest).toByteBuffer), server.address)
      }
  }

  private def generateTransactionId(): ByteVector = {
    val buffer = new Array[Byte](12)
    ThreadLocalRandom.current().nextBytes(buffer)
    ByteVector(buffer)
  }

  private def localAddresses(): Seq[InetAddress] = {
    val localInterfaces = NetworkInterface.getNetworkInterfaces.toSeq
    localInterfaces.flatMap(_.getInetAddresses.toSeq).toVector
  }

}

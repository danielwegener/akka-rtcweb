package akka.rtcweb.protocol.ice

import java.net.{ InetAddress, InetSocketAddress, NetworkInterface }

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.io.Udp
import akka.rtcweb.protocol.ice.IceAgent.{ GatherCandidates, OnIceCandidate }
import akka.rtcweb.protocol.ice.stun.StunMessage
import akka.rtcweb.protocol.jsep.RTCPeerConnection.StunServerDescription
import akka.util.ByteString
import scodec.bits.{ BitVector, ByteVector }

import scala.collection.JavaConversions._
import scala.collection.immutable.Seq
import scala.concurrent.forkjoin.ThreadLocalRandom

object IceAgent {

  def props(listener: ActorRef, iceServers: Vector[StunServerDescription]) = Props(new IceAgent(listener, iceServers))

  final case class OnIceCandidate(candidates: Seq[InetSocketAddress])
  object GatherCandidates

}

class IceAgent private (listener: ActorRef, iceServers: Vector[StunServerDescription]) extends Actor with ActorLogging {

  require(iceServers.nonEmpty, "iceServers must not be empty")

  var outstandingTransactionIds: Vector[(BitVector, StunServerDescription)] = Vector.empty

  override def receive: Receive = {
    case Udp.Bound(localWildcardAddress) if localWildcardAddress.getAddress.isAnyLocalAddress =>
      context.become(ready(sender(), localAddresses(), localWildcardAddress.getPort))
    case Udp.Bound(localAddress) =>
      context.become(ready(sender(), Vector(localAddress.getAddress), localAddress.getPort))
  }

  def ready(socket: ActorRef, localAddresses: Seq[InetAddress], port: Int): Receive = {
    case Udp.Received(data, sender) =>
      val decoded = StunMessage.codec.complete.decode(ByteVector.view(data.asByteBuffer).bits).require
      log.info(s"Received a decoded stun message: $decoded")
    case GatherCandidates =>
      listener ! OnIceCandidate(localAddresses.map(address => new InetSocketAddress(address, port)))
      iceServers.foreach { server =>
        val stunBindingRequest = StunMessage(stun.Class.request, stun.Method.Binding, generateTransactionId())
        val byteBuffer = ByteString.fromByteBuffer(StunMessage.codec.encode(stunBindingRequest).require.toByteBuffer).compact
        socket ! Udp.Send(byteBuffer, server.address)
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

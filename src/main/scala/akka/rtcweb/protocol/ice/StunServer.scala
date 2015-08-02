package akka.rtcweb.protocol.ice

import java.net.InetSocketAddress

import akka.actor.{ ActorLogging, ActorRef, Props, Actor }
import akka.io.{ Udp, IO }
import akka.rtcweb.protocol.ice.stun._
import akka.util.ByteString
import scodec.Attempt.{ Failure, Successful }
import scodec.DecodeResult
import scodec.bits.BitVector

object StunServer {

  def props(port: Option[Int] = Some(3478)) = Props(new StunServer(new InetSocketAddress("localhost", port.getOrElse(0))))
  val knownAttributeTypes: Set[StunAttributeType] = Set(StunAttributeType.`ERROR-CODE`)
}

class StunServer(socketAddress: InetSocketAddress) extends Actor with ActorLogging {

  import context.system
  IO(Udp) ! Udp.Bind(self, socketAddress)

  override def receive = {
    case Udp.Bound(local) =>
      log.info(s"StunServer bound to $local")
      context.become(ready(sender(), local.getPort))
  }

  val codec = StunMessage.codec
  val software = SOFTWARE("Akka the network monster")

  def ready(socket: ActorRef, actualPort: Int): Receive = {
    case Udp.Received(data, remote) =>
      val decoded = codec.decode(BitVector(data.asByteBuffer))
      decoded match {
        case Successful(DecodeResult(StunMessage(Class.request, Method.Binding, transactionId, attribute), _)) =>
          val unknownAttributeTypes = attribute.map(_.attributeType).filterNot(StunServer.knownAttributeTypes.contains)
          val missingMandatoryAttribute = false //todo: detect attributes that must be understand (type < 0x7fff)
          val mappedAddress = `XOR-MAPPED-ADDRESS`(Family.fromAddress(remote.getAddress), remote.getPort, remote.getAddress)
          val response = StunMessage(Class.successResponse, Method.Binding, transactionId, Vector(mappedAddress, software))
          val responseEncoded = ByteString(bytes = codec.encode(response).require.toByteBuffer)
          log.debug(s"Received a binding request from ${remote.getAddress}:${remote.getPort} with id $transactionId. Responding with $response")

          socket ! Udp.Send(data, remote)
        case Successful(other) => log.warning(s"Received a unprocessable request from ${remote.getAddress}: $other")
        case Failure(cause) => log.warning(s"Could not decode request from ${remote.getAddress}: $cause")
      }

    case Udp.Unbind => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }

}

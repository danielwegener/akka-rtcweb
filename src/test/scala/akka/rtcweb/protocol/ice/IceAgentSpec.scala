package akka.rtcweb.protocol.ice

import java.net.{ InetAddress, InetSocketAddress }

import akka.actor.ActorSystem
import akka.io.Udp
import akka.rtcweb.protocol.ice.IceAgent.{ AgentRole, OnIceCandidate }
import akka.rtcweb.protocol.ice.stun.{ Class, Method, StunMessage }
import akka.rtcweb.protocol.jsep.RTCPeerConnection.StunServerDescription
import akka.testkit._
import akka.util.{ ByteString, Timeout }
import org.specs2.mutable.Specification
import scodec.bits.BitVector

import scala.concurrent.duration._

class IceAgentSpec extends Specification with TestKitBase {
  10.milliseconds.dilated

  sequential

  override implicit lazy val system: ActorSystem = ActorSystem("IceAgentSpec")
  implicit val timeout = Timeout(10.seconds)
  implicit val context = system.dispatcher

  val udpSender = new InetSocketAddress(InetAddress.getLoopbackAddress, 1337)

  "IceAgent" should {

    "go on a discovery" in {
      val remoteHost = new InetSocketAddress("10.11.12.13", 4040)

      val unitRef = TestActorRef[IceAgent](IceAgent.props(AgentRole.Controlling, testActor, Vector(StunServerDescription(remoteHost))))
      val udpTestProbe = TestProbe()
      val clientTestProbe = TestProbe()
      val localPort = new InetSocketAddress("::", 4242)

      unitRef.receive(Udp.Bound(localPort), udpTestProbe.ref)
      unitRef.receive(IceAgent.GatherCandidates, clientTestProbe.ref)

      val stunRequest = udpTestProbe.expectMsgPF() {
        case Udp.Send(payload, target, ack) if target == remoteHost =>
          StunMessage.codec.decode(BitVector(payload.asByteBuffer)).require.value
      }

      val stunResponse = StunMessage(Class.successResponse, Method.Binding, stunRequest.transactionId)
      unitRef.receive(Udp.Received(ByteString(StunMessage.codec.encode(stunResponse).require.toByteBuffer), remoteHost), udpTestProbe.ref)

      clientTestProbe.expectMsgClass(5.seconds, classOf[OnIceCandidate])
      clientTestProbe.expectMsgClass(5.seconds, classOf[OnIceCandidate])
      success
    }
  }

  // cleanup there
  step(shutdown())

}

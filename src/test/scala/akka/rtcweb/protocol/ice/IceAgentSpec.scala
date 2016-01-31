package akka.rtcweb.protocol.ice

import java.net.{ InetAddress, InetSocketAddress }

import akka.actor.ActorSystem
import akka.io.Udp
import akka.rtcweb.protocol.ice.IceAgent.{ OnIceGatheringStateChange, AgentRole, OnIceCandidate }
import akka.rtcweb.protocol.ice.stun.{ StunAgent, Class, Method, StunMessage }
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

    "go out and find server reflexive candidates" in {
      val stunServers = Vector(
        new InetSocketAddress("stun.l.google.com", 19302)
      //new InetSocketAddress("stun1.l.google.com", 19302)
      ).map(StunServerDescription(_, None))

      val clientTestProbe = TestProbe()
      val portRange = 1024 to 2048
      val unitRef = TestActorRef[IceAgent](IceAgent.props(AgentRole.Controlling, clientTestProbe.ref, stunServers, portRange))

      clientTestProbe.fishForMessage(5.seconds, "an server reflexive candidate") {
        case OnIceGatheringStateChange(_) => false
        case OnIceCandidate(Candidate(_, _, Transport.UDP, _, _, CandidateType.ServerReflexiveCandidate, _, _)) => true
      }
      success
    }
  }

  // cleanup there
  step(shutdown())

}

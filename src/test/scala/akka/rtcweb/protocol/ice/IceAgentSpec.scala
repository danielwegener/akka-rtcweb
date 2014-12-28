package akka.rtcweb.protocol.ice

import java.net.{ InetAddress, InetSocketAddress }

import akka.actor.ActorSystem
import akka.io.{ IO, Udp }
import akka.pattern.ask
import akka.rtcweb.protocol.ice.IceAgent.OnIceCandidate
import akka.rtcweb.protocol.jsep.RTCPeerConnection.StunServerDescription
import akka.testkit._
import akka.util.Timeout
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

class IceAgentSpec extends TestKitBase
    with WordSpecLike with Matchers with BeforeAndAfterAll
    with Inspectors with OptionValues with ImplicitSender {
  10.milliseconds.dilated

  implicit lazy val system = ActorSystem()
  implicit val timeout = Timeout(10 seconds)
  implicit val context = system.dispatcher

  val udpSender = new InetSocketAddress(InetAddress.getLoopbackAddress, 1337)

  override def afterAll() {
    shutdown()
  }

  "IceAgent" should {

    "go on a discovery" in {
      val unitRef = TestActorRef[IceAgent](IceAgent.props(Vector(StunServerDescription(new InetSocketAddress("74.125.136.127", 19302)))))

      IO(Udp).!(Udp.Bind(unitRef, new InetSocketAddress("::", 0)))(unitRef)
      Thread.sleep(1000)
      unitRef ! IceAgent.GatherCandidates
      expectMsgClass(5 seconds, classOf[OnIceCandidate])
      expectMsgClass(5 seconds, classOf[OnIceCandidate])

    }
  }

}

package akka.rtcweb

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.ActorSystem
import akka.io.Udp
import akka.testkit._
import akka.util.ByteString
import org.scalatest._
import scala.concurrent.duration._

class UdpMuxerSpec extends TestKit(ActorSystem("UdpMuxerSpec"))
  with DefaultTimeout with WordSpecLike with Matchers with BeforeAndAfterAll
  with Inspectors with OptionValues with ImplicitSender{

  val udpSender = new InetSocketAddress(InetAddress.getLoopbackAddress,1337)

  override def afterAll() {
    shutdown()
  }

  "UdpMuxer" should {

    "forward an UDP.Received only the first children whose predicate matches the payload" in {
      val client1 = TestProbe()
      val client2 = TestProbe()
      val socket = TestProbe()
      val unitRef = TestActorRef[UdpMuxer](UdpMuxer.props(List( ( {a:ByteString => false}, client1.ref), ({a:ByteString => true}, client2.ref))))
      socket.send(unitRef, Udp.Bound(InetSocketAddress.createUnresolved("foo.de",123)))
      client1.expectMsgClass(1 millisecond, classOf[Udp.Bound])
      client2.expectMsgClass(1 millisecond, classOf[Udp.Bound])
      socket.send(unitRef, Udp.Received(ByteString("foo"),udpSender))
      client1.expectNoMsg()
      client2.expectMsgClass(1 millisecond, classOf[Udp.Received])
    }

    "if the socket sends an UDP.Unbound it should be propagated to the client actors" in {
      val client1 = TestProbe()
      val client2 = TestProbe()
      val socket = TestProbe()
      val unitRef = TestActorRef[UdpMuxer](UdpMuxer.props(List( ( {a:ByteString => false}, client1.ref), ({a:ByteString => true}, client2.ref))))
      socket.send(unitRef , Udp.Bound(InetSocketAddress.createUnresolved("foo.de",123)))
      client1.expectMsgClass(1 millisecond, classOf[Udp.Bound])
      client2.expectMsgClass(1 millisecond, classOf[Udp.Bound])
      socket.send(unitRef , Udp.Unbound)
      client1.expectMsgClass(1 millisecond, classOf[Udp.Unbound])
      client2.expectMsgClass(1 millisecond, classOf[Udp.Unbound])
    }

    "if any sender sends an UDP.Unbind it should be propagated to the socket actor" in {
      val socket = TestProbe()
      val unitRef = TestActorRef[UdpMuxer](UdpMuxer.props(Nil))
      socket.send(unitRef, Udp.Bound(InetSocketAddress.createUnresolved("foo.de",123)))
      unitRef ! Udp.Unbind
      socket.expectMsg(1 millisecond, Udp.Unbind)
    }

  }


}

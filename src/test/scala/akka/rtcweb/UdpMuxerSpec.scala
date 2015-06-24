package akka.rtcweb

import java.net.{ InetAddress, InetSocketAddress }

import akka.actor.{ ActorSystem, Terminated }
import akka.io.Udp
import akka.testkit._
import akka.util.ByteString
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class UdpMuxerSpec extends Specification with TestKitBase {
  implicit val system = ActorSystem()
  10.milliseconds.dilated

  val udpSender = new InetSocketAddress(InetAddress.getLoopbackAddress, 1337)

  "UdpMuxer" should {

    "forward an UDP.Received only to the first client whose predicate matches the payload" in {
      val client1 = TestProbe()
      val client2 = TestProbe()
      val socket = TestProbe()
      val unitRef = TestActorRef[UdpMuxer](UdpMuxer.props(List(({ a: ByteString => false }, client1.ref), ({ a: ByteString => true }, client2.ref))))
      unitRef.underlying.become(unitRef.underlyingActor.ready(socket.ref, InetSocketAddress.createUnresolved("0.0.0.0", 123)))
      socket.send(unitRef, Udp.Received(ByteString("foo"), udpSender))
      client2.expectMsgClass(1 millisecond, classOf[Udp.Received])
      client1.expectNoMsg(1 millisecond)
      success
    }

    "accept a single Udp.Bound message before entering normal operation and forward this message to its clients" in {
      val socket = TestProbe()
      val client1 = TestProbe()
      val unit = TestActorRef[UdpMuxer](UdpMuxer.props(List(({ a: ByteString => false }, client1.ref))))
      val boundMessage = Udp.Bound(InetSocketAddress.createUnresolved("0.0.0.0", 123))
      socket.send(unit, boundMessage)
      client1.expectMsg(boundMessage)
      success
    }

    "if the socket sends an UDP.Unbound it should terminate itself and propagated the UDP.Unbound to the client actors" in {
      val client1 = TestProbe()
      val client2 = TestProbe()
      val socket = TestProbe()
      val unitRef = TestActorRef[UdpMuxer](UdpMuxer.props(List(({ a: ByteString => false }, client1.ref), ({ a: ByteString => true }, client2.ref))))
      unitRef.underlying.become(unitRef.underlyingActor.ready(socket.ref, InetSocketAddress.createUnresolved("0.0.0.0", 123)))
      watch(unitRef)
      socket.send(unitRef, Udp.Unbound)
      client1.expectMsgClass(1 millisecond, classOf[Udp.Unbound])
      client2.expectMsgClass(1 millisecond, classOf[Udp.Unbound])
      expectMsgPF() { case Terminated(subject) if subject == unitRef => true }
      success
    }

    "if any sender sends an UDP.Unbind it should be propagated to the socket actor" in {
      val socket = TestProbe()
      val unitRef = TestActorRef[UdpMuxer](UdpMuxer.props(Nil))
      socket.send(unitRef, Udp.Bound(InetSocketAddress.createUnresolved("foo.de", 123)))
      unitRef ! Udp.Unbind
      socket.expectMsg(1 millisecond, Udp.Unbind)
      success
    }

  }

  step(shutdown())

}

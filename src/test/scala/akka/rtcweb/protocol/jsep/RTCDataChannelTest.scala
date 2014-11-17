package akka.rtcweb.protocol.jsep

import akka.actor.{ Terminated, ActorSystem }
import akka.testkit.{ TestProbe, DefaultTimeout, TestKit, TestActorRef }
import org.scalatest.{ BeforeAndAfterAll, WordSpecLike, Matchers }

class RTCDataChannelTest extends TestKit(ActorSystem("RTCDataChannelTest")) with DefaultTimeout with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll() { shutdown() }

  val listenerProbe = TestProbe()
  val unitRef = TestActorRef[RTCDataChannel](RTCDataChannel.props(listenerProbe.ref, RTCPeerConnection.RTCDataChannelInit.DEFAULT))
  val unit = unitRef.underlyingActor

  "RTCDataChannel" should {
    "do!" in {
      unit.receive("foo")
    }

  }

}

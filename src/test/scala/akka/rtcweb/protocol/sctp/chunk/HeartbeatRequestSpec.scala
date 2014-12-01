package akka.rtcweb.protocol.sctp.chunk

import org.scalatest.{Inside, Matchers, WordSpec}
import scodec.bits._
import scalaz.{-\/, \/-}

class HeartbeatRequestSpec extends WordSpec with Matchers with Inside {

  "An HeartBeatRequest" should {
    "be pickable back and forth" in {
      val in = HeartbeatRequest(hex"affe")
      val decoded = HeartbeatRequest.codec.encode(in).flatMap(HeartbeatRequest.codec.decode)

      inside(decoded) {
        case \/-((BitVector.empty, out)) => in should be(out)
        case -\/(e) => fail(e.message)
      }
    }
  }

}

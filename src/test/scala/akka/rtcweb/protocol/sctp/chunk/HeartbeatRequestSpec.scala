package akka.rtcweb.protocol.sctp.chunk

import org.scalatest.{ Inside, Matchers, WordSpec }
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits._

class HeartbeatRequestSpec extends WordSpec with Matchers with Inside {

  "An HeartBeatRequest" should {
    "be pickable back and forth" in {
      val in = HeartbeatRequest(hex"affe")
      val decoded = HeartbeatRequest.codec.encode(in).flatMap(HeartbeatRequest.codec.decode)

      decoded shouldBe Successful(DecodeResult(in, BitVector.empty))

    }
  }

}

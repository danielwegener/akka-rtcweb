package akka.rtcweb.protocol.sctp.chunk

import akka.rtcweb.CodecSpec
import org.specs2.mutable.Specification
import scodec.bits._

class HeartbeatRequestSpec extends Specification with CodecSpec {

  "An HeartBeatRequest" should {
    "be encodable back and forth" in {
      roundtrip(HeartbeatRequest(hex"affe"))
    }
  }

}

package akka.rtcweb.protocol.ice.stun

import org.scalatest.{Matchers, WordSpecLike}
import scodec.bits._

class StunAttributeSpec extends WordSpecLike with Matchers {

  "XOR-MAPPED-ADDRESS" should {
    "decode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.encodeValid(50768) should be(hex"e742")
    }

  }

}

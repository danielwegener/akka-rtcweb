package akka.rtcweb.protocol.ice.stun

import org.scalatest.{ Inside, Matchers, WordSpec }
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits._
import shapeless.HNil

class StunMessageSpec extends WordSpec with Matchers with Inside {

  "StunMessageHeader" should {
    "decode funky encoded stun message type" in {
      StunMessage.stunMessageTypeBitCodec.decode(bin"11000100011001") shouldBe
        Successful(DecodeResult(bin"110000001001" :: bin"11" :: HNil, BitVector.empty))
    }

    "encode funky encoded stun message type" in {
      StunMessage.stunMessageTypeBitCodec.encode(bin"000000000000" :: bin"11" :: HNil) shouldBe
        Successful(bin"00000100010000")
    }

    "decode funky encoded stun message type in types" in {
      StunMessage.stunMessageTypeCodec.decode(bin"00000000010001") shouldBe
        Successful(DecodeResult(Class.indication :: Method.Binding :: HNil, BitVector.empty))
    }

    "encode funky stun message types" in {
      StunMessage.stunMessageTypeCodec.encode(Class.errorResponse :: Method.Binding :: HNil) shouldBe
        Successful(bin"00000100010001")
    }

  }

}

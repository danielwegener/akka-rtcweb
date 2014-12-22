package akka.rtcweb.protocol.ice.stun

import org.scalatest.{ Inside, Matchers, WordSpec }
import scodec.bits._
import shapeless.HNil
import scala.language.postfixOps

import scalaz.{ \/-, -\/ }

class StunMessageSpec extends WordSpec with Matchers with Inside {

  "StunMessageHeader" should {
    "decode funky encoded stun message type" in {
      inside(StunMessage.stunMessageTypeBitCodec.decode(bin"11000100011001")) {
        case \/-((rest, chunkType)) =>
          rest should be(BitVector.empty)
          chunkType should be(bin"110000001001" :: bin"11" :: HNil)
        case e => fail(e.toString)
      }
    }

    "encode funky encoded stun message type" in {
      inside(StunMessage.stunMessageTypeBitCodec.encode(bin"000000000000" :: bin"11" :: HNil)) {
        case \/-(bits) =>
          bits should be(bin"00000100010000")
        case e => fail(e.toString)
      }
    }

    "decode funky encoded stun message type in types" in {
      inside(StunMessage.stunMessageTypeCodec.decode(bin"00000000010001")) {
        case \/-((rest, chunkType)) =>
          rest should be(BitVector.empty)
          chunkType should be(Class.indication :: Method.Binding :: HNil)
        case e => fail(e.toString)
      }
    }

    "encode funky stun message types" in {
      inside(StunMessage.stunMessageTypeCodec.encode(Class.errorResponse :: Method.Binding :: HNil)) {
        case \/-(bits) =>
          bits should be(bin"00000100010001")
        case e => fail(e.toString)
      }
    }

  }

}

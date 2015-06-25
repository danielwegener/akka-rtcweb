package akka.rtcweb.protocol.ice.stun

import java.net.InetAddress

import akka.rtcweb.CodecSpec
import akka.rtcweb.protocol.ice.stun.`ERROR-CODE`.Code
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits._
import shapeless.HNil

class StunMessageSpec extends org.specs2.mutable.Specification with CodecSpec {

  "StunMessage" should {

    "encoding roundtrip with a complex message" in {
      roundtrip(
        StunMessage(Class.request, Method.Binding, hex"0x000000000000000000000001", Vector(
          `MAPPED-ADDRESS`(Family.IPv4, 42, InetAddress.getLoopbackAddress),
          `ALTERNATE-SERVER`(Family.IPv4, 43, InetAddress.getLoopbackAddress),
          `XOR-MAPPED-ADDRESS`(Family.IPv4, 42, InetAddress.getLoopbackAddress),
          `ERROR-CODE`(Code.UNKNOWN(501), "bad is bad"),
          `UNKNOWN-ATTRIBUTES`(Vector(StunAttributeType.NONCE, StunAttributeType.UNKNOWN(hex"0x4242".bits))),
          SOFTWARE("MY AWESOME SOFTWARE 1.0"),
          `USE-CANDIDATE`(),
          PRIORITY(Integer.MAX_VALUE),
          `ICE-CONTROLLING`(hex"0x1223344556677889")
        )))
    }

  }

  "StunMessageHeader" should {
    "decode funky encoded stun message type" in {
      StunMessage.stunMessageTypeBitCodec.decode(bin"11000100011001") shouldEqual
        Successful(DecodeResult(bin"110000001001" :: bin"11" :: HNil, BitVector.empty))
    }

    "encode funky encoded stun message type" in {
      StunMessage.stunMessageTypeBitCodec.encode(bin"000000000000" :: bin"11" :: HNil) shouldEqual
        Successful(bin"00000100010000")
    }

    "decode funky encoded stun message type in types" in {
      StunMessage.stunMessageTypeCodec.decode(bin"00000000010001") shouldEqual
        Successful(DecodeResult(Class.indication :: Method.Binding :: HNil, BitVector.empty))
    }

    "encode funky stun message types" in {
      StunMessage.stunMessageTypeCodec.encode(Class.errorResponse :: Method.Binding :: HNil) shouldEqual
        Successful(bin"00000100010001")
    }

  }

}

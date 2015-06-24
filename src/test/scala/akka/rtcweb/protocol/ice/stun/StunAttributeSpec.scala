package akka.rtcweb.protocol.ice.stun

import java.net.InetAddress

import akka.rtcweb.CodecSpec
import org.specs2.mutable.Specification
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits._

class StunAttributeSpec extends Specification with CodecSpec {

  "XOR-MAPPED-ADDRESS" should {

    "roundtrip" in {
      roundtrip(`XOR-MAPPED-ADDRESS`(Family.IPv4, 42, InetAddress.getLoopbackAddress))
    }

    "encode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.encode(50768) should beEqualTo(Successful(hex"e742".bits))
      `XOR-MAPPED-ADDRESS`.xPortCodec.encode(50720) should beEqualTo(Successful(hex"e732".bits))
    }

    "decode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.decode(hex"e7420001".bits) should beEqualTo(Successful(DecodeResult(50768, hex"0001".bits)))
      `XOR-MAPPED-ADDRESS`.xPortCodec.decode(hex"e732".bits) should beEqualTo(Successful(DecodeResult(50720, BitVector.empty)))
    }
  }

}

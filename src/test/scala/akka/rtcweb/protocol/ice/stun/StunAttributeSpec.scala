package akka.rtcweb.protocol.ice.stun

import org.scalatest.{ Matchers, WordSpecLike }
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits._
import scalaz.\/-

class StunAttributeSpec extends WordSpecLike with Matchers {

  "XOR-MAPPED-ADDRESS" should {
    "encode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.encode(50768) should be(Successful(hex"e742".bits))
      `XOR-MAPPED-ADDRESS`.xPortCodec.encode(50720) should be(Successful(hex"e732".bits))
    }

    "decode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.decode(hex"e7420001".bits) should be(Successful(DecodeResult(50768, hex"0001".bits)))
      `XOR-MAPPED-ADDRESS`.xPortCodec.decode(hex"e732".bits) should be(Successful(DecodeResult(50720, BitVector.empty)))
    }

  }

}

package akka.rtcweb.protocol.ice.stun

import org.scalatest.{Matchers, WordSpecLike}
import scodec.bits._
import scalaz.\/-

class StunAttributeSpec extends WordSpecLike with Matchers {

  "XOR-MAPPED-ADDRESS" should {
    "encode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.encodeValid(50768) should be(hex"e742".bits)
      `XOR-MAPPED-ADDRESS`.xPortCodec.encodeValid(50720) should be(hex"e732".bits)
    }

    "decode X-PORT" in {
      `XOR-MAPPED-ADDRESS`.xPortCodec.decode(hex"e7420001".bits) should be(\/-(hex"0001".bits, 50768))
      `XOR-MAPPED-ADDRESS`.xPortCodec.decodeValidValue(hex"e732".bits) should be(50720)
    }

  }

}

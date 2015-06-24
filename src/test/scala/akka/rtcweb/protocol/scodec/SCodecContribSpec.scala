package akka.rtcweb.protocol.scodec

import org.specs2.mutable.Specification
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits.BitVector.{ empty => emptyVector }
import scodec.bits.BitVector._
import scodec.bits._
import scodec.codecs._
import shapeless.HNil

import scala.concurrent.duration._

class SCodecContribSpec extends Specification {

  import SCodecContrib._

  "blockalignBits" should {
    "decode an uint8 followed by a two bit padding" in {
      val res = blockalignBits(uint8, 9).decode(uint8.encode(1).require ++ low(1))
      res shouldEqual Successful(DecodeResult(1, emptyVector))
    }

    "encode an uint8 followed by a two bit padding" in {
      val res = blockalignBits(uint8 :: uint8, 9).encode(1 :: 1 :: HNil)
      res shouldEqual Successful(low(7) ++ high(1) ++ low(7) ++ high(1) ++ low(2))
    }
  }

  "blockalignBytes" should {
    "encode" in {
      val res = blockalignBytes(uint8 :: uint8 :: uint8, 2).encode(255 :: 255 :: 255 :: HNil)
      res shouldEqual Successful(high(24) ++ low(8))
    }

    "decode" in {
      blockalignBytes(uint8, 2).decode(uint8.encode(1).require ++ high(9)) shouldEqual
        Successful(DecodeResult(1, high(1)))
    }
  }

  "duration" should {
    "encode 42 Minutes" in {
      duration(uint8, concurrent.duration.MINUTES).encode(42 minutes) shouldEqual
        Successful(uint8.encode(42).require)
    }

    "decode 42 Minutes" in {
      duration(uint8, concurrent.duration.MINUTES).decode(uint8.encode(42).require) shouldEqual
        Successful(DecodeResult(42 minutes, BitVector.empty))
    }
  }

  "multiVariableSize" should {

    "decode" in {
      multiVariableSizes(uint8 :: uint8 :: HNil, ascii :: ascii :: HNil).decode(hex"0102414142".bits) shouldEqual
        Successful(DecodeResult("A" :: "AB" :: HNil, BitVector.empty))
    }

    "encode" in {
      multiVariableSizes(uint8 :: uint8 :: HNil, ascii :: ascii :: HNil).encode("A" :: "AB" :: HNil) shouldEqual
        Successful(hex"0102414142".bits)
    }

  }

}

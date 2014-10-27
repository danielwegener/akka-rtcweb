package akka.rtcweb.protocol.scodec

import org.scalatest.{ Inside, Matchers, WordSpec }
import scodec.bits.BitVector.{ empty => emptyVector }
import scodec.bits.BitVector._
import scodec.codecs._
import shapeless.HNil

import scalaz.{ \/-, -\/ }

class SCodecContribTest extends WordSpec with Matchers with Inside {

  import SCodecContrib._

  "alignBits" should {
    "decode an uint8 followed by a two bit padding" in {
      val res = alignBits(uint8, 9).decode(uint8.encodeValid(1) ++ low(1))
      inside(res) {
        case \/-((rest, result)) =>
          rest should be(emptyVector)
          result should be(1)
      }
    }

    "encode an uint8 followed by a two bit padding" in {
      val res = alignBits(uint8 :: uint8, 9).encode(1 :: 1 :: HNil)
      inside(res) {
        case \/-(result) =>
          result should be(low(7) ++ high(1) ++ low(7) ++ high(1) ++ low(2))

      }
    }
  }

  "alignBytes" should {
    "encode" in {
      val res = alignBytes(uint8 :: uint8 :: uint8, 2).encode(255 :: 255 :: 255 :: HNil)
      inside(res) {
        case \/-(result) =>
          result should be(high(24) ++ low(8))
      }
    }

    "decode" in {
      val res = alignBytes(uint8, 2).decode(uint8.encodeValid(1) ++ high(9))
      inside(res) {
        case \/-((rest, result)) =>
          rest should be(high(1))
          result should be(1)
      }
    }
  }

}

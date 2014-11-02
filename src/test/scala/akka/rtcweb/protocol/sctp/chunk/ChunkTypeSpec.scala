package akka.rtcweb.protocol.sctp.chunk

import org.scalatest.{Inside, Matchers, WordSpec}
import scodec.bits.BitVector

import scalaz.\/-

class ChunkTypeSpec extends WordSpec with Matchers with Inside {

  "ChunkType" should {
    "be decodable" in {
      inside(ChunkType.codec.decode(BitVector.fromInt(0,8))) {
        case \/-((rest, chunkType)) =>
          rest should be(BitVector.empty)
          chunkType should be(ChunkType.DATA)
      }
    }

    "decode unknown chunkTypes" in {
      inside(ChunkType.codec.decode(BitVector.fromInt(42,8))) {
        case \/-((rest, chunkType)) =>
          rest should be(BitVector.empty)
          chunkType should be(ChunkType.UNKNOWN(42))
      }
    }
  }

}

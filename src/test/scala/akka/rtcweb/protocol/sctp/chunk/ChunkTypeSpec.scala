package akka.rtcweb.protocol.sctp.chunk

import org.scalatest.{ Inside, Matchers, WordSpec }
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits.BitVector

class ChunkTypeSpec extends WordSpec with Matchers with Inside {

  "ChunkType" should {
    "be decodable" in {
      ChunkType.codec.decode(BitVector.fromInt(0, 8)) shouldBe
        Successful(DecodeResult(ChunkType.DATA, BitVector.empty))
    }

    "decode unknown chunkTypes" in {
      ChunkType.codec.decode(BitVector.fromInt(42, 8)) shouldBe
        Successful(DecodeResult(ChunkType.UNKNOWN(42), BitVector.empty))
    }
  }
}

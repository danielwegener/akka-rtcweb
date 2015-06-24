package akka.rtcweb.protocol.sctp.chunk

import org.specs2.mutable.Specification
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits.BitVector

class ChunkTypeSpec extends Specification {

  "ChunkType" should {
    "be decodable" in {
      ChunkType.codec.decode(BitVector.fromInt(0, 8)) shouldEqual
        Successful(DecodeResult(ChunkType.DATA, BitVector.empty))
    }

    "decode unknown chunkTypes" in {
      ChunkType.codec.decode(BitVector.fromInt(42, 8)) shouldEqual
        Successful(DecodeResult(ChunkType.UNKNOWN(42), BitVector.empty))
    }
  }
}

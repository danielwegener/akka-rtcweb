package akka.rtcweb.protocol.sctp.chunk

import akka.rtcweb.protocol.sctp.chunk.Initiation._
import org.scalatest.{Matchers, Inside, WordSpec}
import scodec.bits.ByteVector
import scalaz.\/-

class InitiationTest extends WordSpec with Matchers with Inside {

  "Initiation" should {
    "be encodable" ignore {
      val in = Initiation(1,1,1,1,1,Vector[InitiationParameter](`IPv4 Address`(ByteVector.fromByte(1))))
      inside (Initiation.codec.encode(in)) { case \/-(bits) =>
          bits should be("hi")
      }

    }
  }

}

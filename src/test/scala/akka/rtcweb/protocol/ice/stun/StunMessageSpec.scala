package akka.rtcweb.protocol.ice.stun

import org.scalatest.{ Inside, Matchers, WordSpec }
import scodec.bits._
import scala.language.postfixOps

import scalaz.{ \/-, -\/ }

class StunMessageSpec extends WordSpec with Matchers with Inside {

  "StunMessageHeader" should {
    "decode funky encoded stun message type" in {
      inside(StunMessageHeader.stunMessageTypeCodec.decode(bin"11000100011001")) {
        case \/-((rest, chunkType)) =>
          rest should be(BitVector.empty)
          chunkType should be((bin"110000001001",bin"11"))
      }
    }


  }

}

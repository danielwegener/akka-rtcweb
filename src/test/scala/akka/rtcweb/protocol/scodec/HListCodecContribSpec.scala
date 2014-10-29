package akka.rtcweb.protocol.scodec

import org.scalatest.{Matchers, WordSpecLike}
import scodec.bits._
import scodec.codecs._
import shapeless._
import shapeless.test.illTyped

class HListCodecContribSpec extends WordSpecLike with Matchers {

  import akka.rtcweb.protocol.scodec.HListCodecContrib._

  "HListCodecContribSpec.hzip" should {

    "compile" in {
      val lengths = uint8 :: uint16 :: HNil
      val values = ascii :: utf8 :: HNil
      val x = hzip(lengths, values)

    }

    "not compile" in {
      illTyped("""
        hzip(uint8::uint8:uint16, ascii::ascii)
        """)

    }

  }

}

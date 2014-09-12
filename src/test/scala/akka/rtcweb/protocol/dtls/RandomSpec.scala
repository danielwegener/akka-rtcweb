package akka.rtcweb.protocol.dtls

import akka.rtcweb.protocol.dtls.handshake.Random
import org.scalatest.{ Matchers, WordSpecLike }

class RandomSpec extends WordSpecLike with Matchers {

  "A Random" must {
    "be makeable" in {
      val random = Random.make
    }

  }

}


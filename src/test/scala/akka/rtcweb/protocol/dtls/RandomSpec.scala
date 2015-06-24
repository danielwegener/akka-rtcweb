package akka.rtcweb.protocol.dtls

import akka.rtcweb.protocol.dtls.handshake.Random
import org.specs2.mutable.Specification

class RandomSpec extends Specification {

  "A Random" should {
    "be makeable" in {
      val random = Random.make
      success
    }

  }

}


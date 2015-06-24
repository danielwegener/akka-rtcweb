package akka.rtcweb.protocol.dtls

import org.specs2.mutable.Specification

class MACAlgorithmSpec extends Specification {

  "A MACAlgorithm" should {
    "support hmac_md5" in {
      MACAlgorithm.hmac_md5.instance !== null
    }
    "support hmac_sha1" in {
      MACAlgorithm.hmac_sha1.instance !== null
    }
    "support hmac_sha256" in {
      MACAlgorithm.hmac_sha256.instance !== null
    }
    "support hmac_sha384" in {
      MACAlgorithm.hmac_sha384.instance !== null
    }
    "support hmac_sha512" in {
      MACAlgorithm.hmac_sha512.instance !== null
    }
  }

}


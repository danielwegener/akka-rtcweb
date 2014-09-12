package akka.rtcweb.protocol.dtls

import org.scalatest.{ Matchers, WordSpecLike }

class MACAlgorithmSpec extends WordSpecLike with Matchers {

  "A MACAlgorithm" must {
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


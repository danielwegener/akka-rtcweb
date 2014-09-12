package akka.rtcweb.protocol.dtls

import org.scalatest.{ Matchers, WordSpecLike }

class BulkCipherAlgorithmSpec extends WordSpecLike with Matchers {

  "A BulkCipherAlgorithm" must {
    "support aes" in {
      BulkCipherAlgorithm.aes.cipher() should not be null
    }
    "support 3des" in {
      BulkCipherAlgorithm.`3des`.cipher() should not be null
    }
    "support rc4" ignore {
      //BulkCipherAlgorithm.rc4.cipher() should not be null
    }

  }

}


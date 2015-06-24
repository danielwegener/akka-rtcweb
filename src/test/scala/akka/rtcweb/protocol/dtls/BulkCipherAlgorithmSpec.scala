package akka.rtcweb.protocol.dtls

import org.specs2.mutable.Specification

class BulkCipherAlgorithmSpec extends Specification {

  "A BulkCipherAlgorithm" should {
    "support aes" in {
      BulkCipherAlgorithm.aes.cipher() should not beNull
    }
    "support 3des" in {
      BulkCipherAlgorithm.`3des`.cipher() should not beNull
    }
    "support rc4" in {
      pending
      //BulkCipherAlgorithm.rc4.cipher() should not be null
    }

  }

}


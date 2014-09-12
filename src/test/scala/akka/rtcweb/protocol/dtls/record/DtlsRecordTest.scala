package akka.rtcweb.protocol.dtls.record

import akka.rtcweb.protocol.dtls.ProtocolVersion
import org.scalatest.{ Matchers, WordSpecLike }
import scodec.bits.ByteVector

/**
 * @author Daniel Wegener (Holisticon AG)
 */
class DtlsRecordTest extends WordSpecLike with Matchers {

  "A DtlsRecord" must {
    "be codable forth and back" in {
      val record = DtlsCiphertext(
        typee = ContentType.alert,
        version = ProtocolVersion.`DTLS 1.2`,
        epoch = 0, sequenceNumber = 1, fragment = ByteVector(0x01, 0x02))

      //todo

    }

  }

}

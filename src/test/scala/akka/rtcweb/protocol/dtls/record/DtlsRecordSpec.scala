package akka.rtcweb.protocol.dtls.record

import akka.rtcweb.CodecSpec
import akka.rtcweb.protocol.dtls.ProtocolVersion
import org.specs2.mutable.Specification
import scodec.bits.HexStringSyntax

class DtlsRecordSpec extends Specification with CodecSpec {

  "A DtlsRecord" should {
    "roundtrip DtlsCiphertext" in {
      roundtrip(DtlsCiphertext(
        `type` = ContentType.alert,
        version = ProtocolVersion.`DTLS 1.2`,
        epoch = 0, sequenceNumber = 1, fragment = hex"0x0102")
      )
    }

    "roundtrip DtlsCompressed" in {
      roundtrip(DtlsCompressed(
        `type` = ContentType.alert,
        version = ProtocolVersion.`DTLS 1.2`,
        epoch = 0, sequenceNumber = 1, opaqueFragment = hex"0x0102")
      )
    }

  }

}

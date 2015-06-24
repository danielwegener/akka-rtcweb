package akka.rtcweb.protocol.dtls.record

import akka.rtcweb.CodecSpec
import akka.rtcweb.protocol.dtls.ProtocolVersion
import org.specs2.mutable.Specification

class ProtocolVersionSpec extends Specification with CodecSpec {

  "A ProtocolVersion" should {
    "be pickable forth and back" in {
      roundtrip(ProtocolVersion.`DTLS 1.2`)
      roundtrip(ProtocolVersion.`TLS v1.0`)
      roundtrip(ProtocolVersion.`TLS v1.2`)
    }

  }

}

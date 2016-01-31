package akka.rtcweb.protocol.dtls.record

import akka.rtcweb.CodecSpec
import org.specs2.mutable.Specification

class ContentTypeSpec extends Specification with CodecSpec {

  "ContentType" should {
    "be encodable and decodable" in {
      roundtrip(ContentType.alert)
      roundtrip(ContentType.application_data)
      roundtrip(ContentType.changeCipherSpec)
      roundtrip(ContentType.handshake)
    }

  }

}

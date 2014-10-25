package akka.rtcweb.protocol.dtls.record

import scodec._
import codecs._

sealed trait ContentType

object ContentType {
  case object changeCipherSpec extends ContentType
  case object alert extends ContentType
  case object handshake extends ContentType
  case object application_data extends ContentType

  implicit val codec: Codec[ContentType] = "ContentType" | mappedEnum(uint8,
    changeCipherSpec -> 20,
    alert -> 21,
    handshake -> 22,
    application_data -> 23)

}
package akka.rtcweb.protocol.dtls.handshake

import scodec.Codec
import scodec.codecs._

sealed trait CipherType

object CipherType {

  /**
   * Not supported by Dtls
   */
  case object stream extends CipherType //(0x00)
  case object block extends CipherType //(0x01)
  /**
   * Not supported by Dtls
   */
  case object aead extends CipherType //(0x02)

  implicit val codec: Codec[CipherType] = mappedEnum(uint8,
    stream -> 0x00,
    block -> 0x01)

}
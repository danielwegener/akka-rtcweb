package akka.rtcweb.protocol.srtpoverdtls

import scodec._
import scodec.codecs._
import scodec.bits._

sealed trait SRTPProtectionProfile

object SRTPProtectionProfile {
  case object SRTP_AES128_CM_HMAC_SHA1_80 extends SRTPProtectionProfile
  case object SRTP_AES128_CM_HMAC_SHA1_32 extends SRTPProtectionProfile
  case object SRTP_NULL_HMAC_SHA1_80 extends SRTPProtectionProfile
  case object SRTP_NULL_HMAC_SHA1_32 extends SRTPProtectionProfile

  implicit val codec: Codec[SRTPProtectionProfile] = "SRTPProtectionProfile" | scodec.codecs.mappedEnum(bytes(2),
    SRTP_AES128_CM_HMAC_SHA1_80 -> hex"0001",
    SRTP_AES128_CM_HMAC_SHA1_32 -> hex"0002",
    SRTP_NULL_HMAC_SHA1_80 -> hex"0005",
    SRTP_NULL_HMAC_SHA1_32 -> hex"0006")
}

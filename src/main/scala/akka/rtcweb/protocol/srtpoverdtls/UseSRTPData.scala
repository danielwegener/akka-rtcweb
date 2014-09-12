package akka.rtcweb.protocol.srtpoverdtls

import scodec.codecs._
import scodec.bits._

case class UseSRTPData(
  srtpProtectionProfiles: List[SRTPProtectionProfile],
  srtp_mki: ByteVector)

object UseSRTPData {
  implicit val codec = {
    "SRTPProtectionProfiles" | listOfN(uint16, SRTPProtectionProfile.codec) ::
      variableSizeBytes(uint8, "srtp_mki" | bytes)
  }.as[UseSRTPData]
}

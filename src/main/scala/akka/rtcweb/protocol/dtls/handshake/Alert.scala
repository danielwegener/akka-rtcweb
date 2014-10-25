package akka.rtcweb.protocol.dtls.handshake

import scodec._
import codecs._

case class Alert(
  level: AlertLevel,
  description: AlertDescription)

object Alert {
  implicit val codec = "Alert" | {
    ("alert_level" | AlertLevel.codec) ::
      ("alert_description" | AlertDescription.codec)
  }.as[Alert]
}

sealed trait AlertLevel

object AlertLevel {
  case object Warning extends AlertLevel // = new AlertLevel(1)
  case object Fatal extends AlertLevel //(2)

  implicit val codec: Codec[AlertLevel] = "AlertLevel" | mappedEnum(uint8, Warning -> 1, Fatal -> 2)

}

sealed trait AlertDescription
object AlertDescription {
  case object CloseNotify extends AlertDescription
  case object UnexpectedMessage extends AlertDescription
  case object bad_record_mac extends AlertDescription
  case object decryption_failed_RESERVED extends AlertDescription
  case object record_overflow extends AlertDescription
  case object decompression_failure extends AlertDescription
  case object handshake_failure extends AlertDescription
  case object no_certificate_RESERVED extends AlertDescription
  case object bad_certificate extends AlertDescription
  case object unsupported_certificate extends AlertDescription
  case object certificate_revoked extends AlertDescription
  case object certificate_expired extends AlertDescription
  case object certificate_unknown extends AlertDescription
  case object illegal_parameter extends AlertDescription
  case object unknown_ca extends AlertDescription
  case object access_denied extends AlertDescription
  case object decode_error extends AlertDescription
  case object decrypt_error extends AlertDescription
  case object export_restriction_RESERVED extends AlertDescription
  case object protocol_version extends AlertDescription
  case object insufficient_security extends AlertDescription
  case object internal_error extends AlertDescription
  case object user_canceled extends AlertDescription
  case object no_renegotiation extends AlertDescription
  case object unsupported_extension extends AlertDescription

  implicit val codec: Codec[AlertDescription] = "AlertDescription" | mappedEnum(uint8,
    CloseNotify -> 0,
    UnexpectedMessage -> 10,
    bad_record_mac -> 20,
    decryption_failed_RESERVED -> 21,
    record_overflow -> 22,
    decompression_failure -> 30,
    handshake_failure -> 40,
    no_certificate_RESERVED -> 41,
    bad_certificate -> 42,
    unsupported_certificate -> 43,
    certificate_revoked -> 44,
    certificate_expired -> 45,
    certificate_unknown -> 46,
    illegal_parameter -> 47,
    unknown_ca -> 48,
    access_denied -> 49,
    decode_error -> 50,
    decrypt_error -> 51,
    export_restriction_RESERVED -> 60,
    protocol_version -> 70,
    insufficient_security -> 71,
    internal_error -> 80,
    user_canceled -> 90,
    no_renegotiation -> 100,
    unsupported_extension -> 110)
}
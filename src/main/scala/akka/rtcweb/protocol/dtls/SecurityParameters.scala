package akka.rtcweb.protocol.dtls

import scodec._
import scodec.bits._
import codecs._
import scalaz.\/

import javax.crypto.{ Mac, Cipher }

import akka.rtcweb.protocol.dtls.handshake._

private[dtls] case object SecurityParameters

/**
 * struct {
 * ConnectionEnd          entity;
 * PRFAlgorithm           prf_algorithm;
 * BulkCipherAlgorithm    bulk_cipher_algorithm;
 * CipherType             cipher_type;
 * uint8                  enc_key_length;
 * uint8                  block_length;
 * uint8                  fixed_iv_length;
 * uint8                  record_iv_length;
 * MACAlgorithm           mac_algorithm;
 * uint8                  mac_length;
 * uint8                  mac_key_length;
 * CompressionMethod      compression_algorithm;
 * opaque                 master_secret[48];
 * opaque                 client_random[32];
 * opaque                 server_random[32];
 * } SecurityParameters;
 */
private[dtls] case class SecurityParameters(
  entity: ConnectionEnd,
  prf_algorithm: PRFAlgorithm,
  bulk_cipher_algorithm: BulkCipherAlgorithm,
  cipher_type: CipherType,
  enc_key_length: Int,
  block_length: Int,
  fixed_iv_length: Int,
  record_iv_length: Int,
  mac_algorithm: MACAlgorithm,
  mac_length: Int,
  mac_key_length: Int,
  compression_algorithm: CompressionMethod)

private[dtls] sealed trait ConnectionEnd

private[dtls] object ConnectionEnd {
  implicit val codec = "ConnectionEnd" | mappedEnum(uint8,
    ServerConnectionEnd -> 0,
    ClientConnectionEnd -> 1)

  case object ServerConnectionEnd extends ConnectionEnd
  case object ClientConnectionEnd extends ConnectionEnd
}

private[dtls] sealed trait PRFAlgorithm

private[dtls] object PRFAlgorithm {
  implicit val codec = { "PRFAlgorithm" | mappedEnum(uint8, tls_prf_sha256 -> 0) }

  case object tls_prf_sha256 extends PRFAlgorithm
}

private[dtls] sealed trait BulkCipherAlgorithm { def cipher(): Cipher }

private[dtls] object BulkCipherAlgorithm {

  case object `null` extends BulkCipherAlgorithm { def cipher() = ??? }
  case object rc4 extends BulkCipherAlgorithm { def cipher() = ??? }
  case object `3des` extends BulkCipherAlgorithm { def cipher() = Cipher.getInstance("DESede") }
  case object aes extends BulkCipherAlgorithm { def cipher() = Cipher.getInstance("AES") }

  implicit val codec: Codec[BulkCipherAlgorithm] = "BulkCipherAlgorithm" | mappedEnum(uint8,
    `null` -> 0,
    rc4 -> 1,
    `3des` -> 2,
    aes -> 3)

}

/**
 * see rfc3749
 */
private[dtls] sealed trait CompressionMethod

object CompressionMethod {

  implicit val codec: Codec[CompressionMethod] = "CompressionMethod" | mappedEnum(uint8,
    `null` -> 0,
    DEFLATE -> 1)

  case object `null` extends CompressionMethod //(0x00)
  case object DEFLATE extends CompressionMethod //(0x01)
}

private[dtls] sealed trait MACAlgorithm {
  def instance: Mac
}

private[dtls] object MACAlgorithm {

  implicit val codec: Codec[MACAlgorithm] = "MACAlgorithm" | mappedEnum(uint8,
    `null` -> 0,
    hmac_md5 -> 1,
    hmac_sha1 -> 2,
    hmac_sha256 -> 3,
    hmac_sha384 -> 4,
    hmac_sha512 -> 5)

  case object `null` extends MACAlgorithm { def instance = ??? }
  case object hmac_md5 extends MACAlgorithm { def instance = Mac.getInstance("HmacMD5") }
  case object hmac_sha1 extends MACAlgorithm { def instance = Mac.getInstance("HmacSHA1") }
  case object hmac_sha256 extends MACAlgorithm { def instance = Mac.getInstance("HmacSHA256") }
  case object hmac_sha384 extends MACAlgorithm { def instance = Mac.getInstance("HmacSHA384") }
  case object hmac_sha512 extends MACAlgorithm { def instance = Mac.getInstance("HmacSHA512") }
}

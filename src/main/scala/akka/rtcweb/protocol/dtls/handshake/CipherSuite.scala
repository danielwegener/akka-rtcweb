package akka.rtcweb.protocol.dtls.handshake

import scodec.Codec
import scodec.codecs._
import scodec.bits._

sealed trait CipherSuite

/**
 * The following values define the cipher suite codes used in the
 * ClientHello and ServerHello messages.
 *
 * A cipher suite defines a cipher specification supported in TLS
 * Version 1.2.
 */
object CipherSuite {

  /**
   * TLS_NULL_WITH_NULL_NULL is specified and is the initial state of a
   * TLS connection during the first handshake on that channel, but MUST
   * NOT be negotiated, as it provides no more protection than an
   * unsecured connection.
   */
  case object TLS_NULL_WITH_NULL_NULL extends CipherSuite //(0x00, 0x00)

  /*The following CipherSuite definitions require that the server provide
   an RSA certificate that can be used for key exchange.  The server may
   request any signature-capable certificate in the certificate request
   message. */
  case object TLS_RSA_WITH_NULL_MD5 extends CipherSuite //(0x00, 0x01)
  case object TLS_RSA_WITH_NULL_SHA extends CipherSuite //(0x00, 0x02)
  case object TLS_RSA_WITH_NULL_SHA256 extends CipherSuite //(0x00, 0x3B)
  case object TLS_RSA_WITH_RC4_128_MD5 extends CipherSuite //(0x00, 0x04)
  case object TLS_RSA_WITH_RC4_128_SHA extends CipherSuite //(0x00, 0x05)
  case object TLS_RSA_WITH_3DES_EDE_CBC_SHA extends CipherSuite //(0x00, 0x0A)
  case object TLS_RSA_WITH_AES_128_CBC_SHA extends CipherSuite //(0x00, 0x2F)
  case object TLS_RSA_WITH_AES_256_CBC_SHA extends CipherSuite //(0x00, 0x35)
  case object TLS_RSA_WITH_AES_128_CBC_SHA256 extends CipherSuite //(0x00, 0x3C)
  case object TLS_RSA_WITH_AES_256_CBC_SHA256 extends CipherSuite //(0x00, 0x3D)

  /*
  The following cipher suite definitions are used for server-
 authenticated (and optionally client-authenticated) Diffie-Hellman.
 DH denotes cipher suites in which the server's certificate contains
 the Diffie-Hellman parameters signed by the certificate authority
 (CA).  DHE denotes ephemeral Diffie-Hellman, where the Diffie-Hellman
 parameters are signed by a signature-capable certificate, which has
 been signed by the CA.  The signing algorithm used by the server is
 specified after the DHE component of the CipherSuite name.  The
 server can request any signature-capable certificate from the client
 for client authentication, or it may request a Diffie-Hellman
 certificate.  Any Diffie-Hellman certificate provided by the client
 must use the parameters (group and generator) described by the
 server.
 */

  case object TLS_DH_DSS_WITH_3DES_EDE_CBC_SHA extends CipherSuite //(0x00, 0x0D)
  case object TLS_DH_RSA_WITH_3DES_EDE_CBC_SHA extends CipherSuite //(0x00, 0x10)
  case object TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA extends CipherSuite //(0x00, 0x13)
  case object TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA extends CipherSuite //(0x00, 0x16)
  case object TLS_DH_DSS_WITH_AES_128_CBC_SHA extends CipherSuite //(0x00, 0x30)
  case object TLS_DH_RSA_WITH_AES_128_CBC_SHA extends CipherSuite //(0x00, 0x31)
  case object TLS_DHE_DSS_WITH_AES_128_CBC_SHA extends CipherSuite //(0x00, 0x32)
  case object TLS_DHE_RSA_WITH_AES_128_CBC_SHA extends CipherSuite //(0x00, 0x33)
  case object TLS_DH_DSS_WITH_AES_256_CBC_SHA extends CipherSuite //(0x00, 0x36)
  case object TLS_DH_RSA_WITH_AES_256_CBC_SHA extends CipherSuite //(0x00, 0x37)
  case object TLS_DHE_DSS_WITH_AES_256_CBC_SHA extends CipherSuite //(0x00, 0x38)
  case object TLS_DHE_RSA_WITH_AES_256_CBC_SHA extends CipherSuite //(0x00, 0x39)
  case object TLS_DH_DSS_WITH_AES_128_CBC_SHA256 extends CipherSuite //(0x00, 0x3E)
  case object TLS_DH_RSA_WITH_AES_128_CBC_SHA256 extends CipherSuite //(0x00, 0x3F)
  case object TLS_DHE_DSS_WITH_AES_128_CBC_SHA256 extends CipherSuite //(0x00, 0x40)
  case object TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 extends CipherSuite //(0x00, 0x67)
  case object TLS_DH_DSS_WITH_AES_256_CBC_SHA256 extends CipherSuite //(0x00, 0x68)
  case object TLS_DH_RSA_WITH_AES_256_CBC_SHA256 extends CipherSuite //(0x00, 0x69)
  case object TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 extends CipherSuite //(0x00, 0x6A)
  case object TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 extends CipherSuite //(0x00, 0x6B)

  /*
  The following cipher suites are used for completely anonymous
   Diffie-Hellman communications in which neither party is
   authenticated.  Note that this mode is vulnerable to man-in-the-
   middle attacks.  Using this mode therefore is of limited use: These
   cipher suites MUST NOT be used by TLS 1.2 implementations unless the
   application layer has specifically requested to allow anonymous key
   exchange.  (Anonymous key exchange may sometimes be acceptable, for
   example, to support opportunistic encryption when no set-up for
   authentication is in place, or when TLS is used as part of more
   complex security protocols that have other means to ensure
   authentication.)
   */

  implicit val codec: Codec[CipherSuite] = "CipherSuite" | mappedEnum(bytes(2),
    TLS_RSA_WITH_NULL_MD5 -> hex"0001",
    TLS_RSA_WITH_NULL_SHA -> hex"0002",
    TLS_RSA_WITH_NULL_SHA256 -> hex"003B",
    TLS_RSA_WITH_RC4_128_MD5 -> hex"0004",
    TLS_RSA_WITH_RC4_128_SHA -> hex"0005",
    TLS_RSA_WITH_3DES_EDE_CBC_SHA -> hex"000A",
    TLS_RSA_WITH_AES_128_CBC_SHA -> hex"002F",
    TLS_RSA_WITH_AES_256_CBC_SHA -> hex"0035",
    TLS_RSA_WITH_AES_128_CBC_SHA256 -> hex"003C",
    TLS_RSA_WITH_AES_256_CBC_SHA256 -> hex"003D",
    TLS_DH_DSS_WITH_3DES_EDE_CBC_SHA -> hex"000D",
    TLS_DH_RSA_WITH_3DES_EDE_CBC_SHA -> hex"0010",
    TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA -> hex"0013",
    TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA -> hex"0016",
    TLS_DH_DSS_WITH_AES_128_CBC_SHA -> hex"0030",
    TLS_DH_RSA_WITH_AES_128_CBC_SHA -> hex"0031",
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA -> hex"0032",
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA -> hex"0033",
    TLS_DH_DSS_WITH_AES_256_CBC_SHA -> hex"0036",
    TLS_DH_RSA_WITH_AES_256_CBC_SHA -> hex"0037",
    TLS_DHE_DSS_WITH_AES_256_CBC_SHA -> hex"0038",
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA -> hex"0039",
    TLS_DH_DSS_WITH_AES_128_CBC_SHA256 -> hex"003E",
    TLS_DH_RSA_WITH_AES_128_CBC_SHA256 -> hex"003F",
    TLS_DHE_DSS_WITH_AES_128_CBC_SHA256 -> hex"0040",
    TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 -> hex"0067",
    TLS_DH_DSS_WITH_AES_256_CBC_SHA256 -> hex"0068",
    TLS_DH_RSA_WITH_AES_256_CBC_SHA256 -> hex"006A",
    TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 -> hex"006B",
    TLS_DH_anon_WITH_RC4_128_MD5 -> hex"0018",
    TLS_DH_anon_WITH_3DES_EDE_CBC_SHA -> hex"001B",
    TLS_DH_anon_WITH_AES_128_CBC_SHA -> hex"0034",
    TLS_DH_anon_WITH_AES_256_CBC_SHA -> hex"003A",
    TLS_DH_anon_WITH_AES_128_CBC_SHA256 -> hex"006C",
    TLS_DH_anon_WITH_AES_256_CBC_SHA256 -> hex"006D")

  case object TLS_DH_anon_WITH_RC4_128_MD5 extends CipherSuite //(0x00, 0x18)
  case object TLS_DH_anon_WITH_3DES_EDE_CBC_SHA extends CipherSuite //(0x00, 0x1B)
  case object TLS_DH_anon_WITH_AES_128_CBC_SHA extends CipherSuite //(0x00, 0x34)
  case object TLS_DH_anon_WITH_AES_256_CBC_SHA extends CipherSuite //(0x00, 0x3A)
  case object TLS_DH_anon_WITH_AES_128_CBC_SHA256 extends CipherSuite //(0x00, 0x6C)
  case object TLS_DH_anon_WITH_AES_256_CBC_SHA256 extends CipherSuite //(0x00, 0x6D)

}
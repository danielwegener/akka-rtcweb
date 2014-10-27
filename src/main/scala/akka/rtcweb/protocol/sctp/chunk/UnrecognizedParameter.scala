package akka.rtcweb.protocol.sctp.chunk

import scodec._
import scodec.bits.ByteVector
import scodec.codecs._
import akka.rtcweb.protocol.scodec.SCodecContrib._

private[sctp] final case class UnrecognizedParameter(parameterType: Int,
  value: ByteVector) extends Parameter

private[sctp] object UnrecognizedParameter {
  implicit val codec: Codec[UnrecognizedParameter] = "Optional Parameter" | {
    ("Parameter Type" | uint8) ::
      ("Parameter Value" | blockalignBytes(variableSizeBytes(uint8, bytes, 4), 4))
  }.as[UnrecognizedParameter]
}
package akka.rtcweb.protocol.sctp

import akka.rtcweb.protocol.sctp.chunk.{ UnrecognizedSctpChunk, PayloadData, Initiation, SctpChunk }
import scodec._
import scodec.bits._
import codecs._

private[sctp] final case class SctpPacket(header: SctpCommonHeader, chunks: Vector[UnrecognizedSctpChunk])

private[sctp] case object SctpPacket {

  implicit val codec = {
    ("common_header" | SctpCommonHeader.codec) ::
      ("chunks" | vector(UnrecognizedSctpChunk.codec))
  }.as[SctpPacket]

}


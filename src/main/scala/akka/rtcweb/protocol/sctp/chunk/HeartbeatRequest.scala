package akka.rtcweb.protocol.sctp.chunk

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

/**
 * HEARTBEAT
 *
 * An endpoint should send this chunk to its peer endpoint to probe the
 * reachability of a particular destination transport address defined in
 * the present association.
 *
 * The parameter field contains the Heartbeat Information, which is a
 * variable-length opaque data structure understood only by the sender.
 *
 *
 */
final case class HeartbeatRequest(heartbeatInformation: ByteVector) extends SctpChunk

object HeartbeatRequest {

  /**
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Type = 4    | Chunk  Flags  |      Heartbeat Length         |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /            Heartbeat Information TLV (Variable-Length)        /
   * \                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec: Codec[HeartbeatRequest] =
    {
      constant(ChunkType.codec.encodeValid(ChunkType.HEARTBEAT)) :~>:
        ignore(8) :~>:
        variableSizeBytes("Heartbeat Length" | uint16,
          "Heartbeat Information TLV" | bytes
        )
    }.as[HeartbeatRequest]

}

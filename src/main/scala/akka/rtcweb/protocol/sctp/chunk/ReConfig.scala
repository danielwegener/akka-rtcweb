package akka.rtcweb.protocol.sctp.chunk

import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

/**
 * from rfc6525
 *
 *
 */
private[sctp] case class ReConfig() extends SctpChunk

object ReConfig {

  sealed trait ReconfigurationParameter extends Parameter
  //todo: Reconfiguration Parameters

  /**
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * | Type = 130    |  Chunk Flags  |      Chunk Length             |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /                  Re-configuration Parameter                   /
   * \                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /             Re-configuration Parameter (optional)             /
   * \                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   *
   * }}}
   */
  implicit val codec: Codec[ReConfig] = {
    "RE-CONFIG" | {
      ("Type" | constant(ChunkType.codec.encodeValid(ChunkType.`RE-CONFIG`))) :~>:
        ("Chunk Flags" | ignore(8)) :~>:
        variableSizeBytes("Chunk Length" | uint16,
          constant(BitVector.empty), 32)

    }.dropUnits.as[ReConfig]

  }
}
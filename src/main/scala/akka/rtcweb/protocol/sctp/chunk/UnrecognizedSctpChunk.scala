package akka.rtcweb.protocol.sctp.chunk

import scodec.Codec
import scodec.bits._
import scodec.codecs._

/**
 * @param chunkType This field identifies the type of information contained in the
 * Chunk Value field.  It takes a value from 0 to 254.  The value of
 * 255 is reserved for future use as an extension field.
 * @param chunkFlags The usage of these bits depends on the Chunk type as given by the
 * Chunk Type field.  Unless otherwise specified, they are set to 0
 * on transmit and are ignored on receipt.
 *
 * Chunks (including Type, Length, and Value fields) are padded out
 * by the sender with all zero bytes to be a multiple of 4 bytes
 * long.  This padding MUST NOT be more than 3 bytes in total.  The
 * Chunk Length value does not include terminating padding of the
 * chunk.  However, it does include padding of any variable-length
 * parameter except the last parameter in the chunk.  The receiver
 * MUST ignore the padding.<br/>
 * Note: A robust implementation should accept the chunk whether or
 * not the final padding has been included in the Chunk Length.
 * @param chunkValue The Chunk Value field contains the actual information to be
 * transferred in the chunk.  The usage and format of this field is
 * dependent on the Chunk Type.
 */
final case class UnrecognizedSctpChunk(
  chunkType: ChunkType,
  chunkFlags: BitVector,
  chunkValue: ByteVector) extends SctpChunk

object UnrecognizedSctpChunk {
  /**
   * The figure below illustrates the field format for the chunks to be
   * transmitted in the SCTP packet.  Each chunk is formatted with a Chunk
   * Type field, a chunk-specific Flag field, a Chunk Length field, and a
   * Value field.
   *
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Chunk Type  | Chunk  Flags  |        Chunk Length           |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /                          Chunk Value                          /
   * \                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   *
   *  - <strong>Chunk Value: variable length</strong>
   *  This value represents the size of the chunk in bytes, including
   * the Chunk Type, Chunk Flags, Chunk Length, and Chunk Value fields.
   * Therefore, if the Chunk Value field is zero-length, the Length
   * field will be set to 4.  The Chunk Length field does not count any
   * chunk padding.<br/>
   *
   */
  implicit val codec: Codec[UnrecognizedSctpChunk] = {
    ("Chunk Type" | ChunkType.codec) ::
      ("Chunk Flags" | bits(8)) ::
      ("Chunk Value" | variableSizeBytes(uint16, bytes, 4))
  }.as[UnrecognizedSctpChunk]
}


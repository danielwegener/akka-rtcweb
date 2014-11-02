package akka.rtcweb.protocol.sctp.chunk

import scodec.Codec
import scodec._
import scodec.codecs._

/**
 * This chunk shall be used by the data sender to inform the data
 * receiver to adjust its cumulative received TSN point forward because
 * some missing TSNs are associated with data chunks that SHOULD NOT be
 * transmitted or retransmitted by the sender.
 *
 * @param newCumulativeTsn This indicates the new cumulative TSN to the data receiver.  Upon
 * the reception of this value, the data receiver MUST consider
 * any missing TSNs earlier than or equal to this value as received,
 * and stop reporting them as gaps in any subsequent SACKs.
 * @param streamTuples A tuple that holds a stream number that was skipped by this
 * FWD-TSN and the sequence number associated with the stream
 * that was skipped.  The stream sequence field holds the largest
 * stream sequence number in this stream being skipped.  The receiver
 * of the FWD-TSN's can use the Stream-N and Stream Sequence-N fields
 * to enable delivery of any stranded TSN's that remain on the stream
 * re-ordering queues.  This field MUST NOT report TSN's corresponding
 * to DATA chunks that are marked as unordered.  For ordered DATA
 * chunks this field MUST be filled in.
 *
 */
private[sctp] final case class ForwardCumulativeTSNChunk(
  newCumulativeTsn: Long,
  streamTuples: Vector[(Int, Int)]) extends SctpChunk

private[sctp] object ForwardCumulativeTSNChunk {

  /**
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Type = 192  |  Flags = 0x00 |        Length = Variable      |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                      New Cumulative TSN                       |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |         Stream-1              |       Stream Sequence-1       |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               /
   * /                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |         Stream-N              |       Stream Sequence-N       |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   * @see [[ForwardCumulativeTSNChunk]]
   */
  implicit val codec: Codec[ForwardCumulativeTSNChunk] = {
    "Forward Cumulative TSN" | {
      constant(ChunkType.codec.encodeValid(ChunkType.`FORWARD TSN`)) :~>:
        ignore(8) :~>:
        variableSizeBytes("Length" | uint16,
          ("New Cumulative TSN" | uint32) ::
            vector(uint16 ~ uint16), 2)

    }.as[ForwardCumulativeTSNChunk]
  }

}

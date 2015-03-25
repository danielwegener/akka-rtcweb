package akka.rtcweb.protocol.sctp.chunk

import scodec.codecs._

/**
 *  This chunk is used to pad an SCTP packet.  A PAD chunk can be used to
 * enlarge the packet by 4 to 65536 bytes in steps of 4 bytes.  An SCTP
 * packet MAY contain multiple PAD chunks.
 * @see [[https://tools.ietf.org/html/rfc4820#section-3 RFC Padding Chunk (PAD)]]
 * @param length This value holds the length of the Padding Data plus 4.
 */
case class PaddingChunk(length: Int) extends SctpChunk

object PaddingChunk {

  /**
   * This chunk is used to pad an SCTP packet.  A PAD chunk can be used to
   * enlarge the packet by 4 to 65536 bytes in steps of 4 bytes.  An SCTP
   * packet MAY contain multiple PAD chunks.
   *
   * {{{
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * | Type = 0x84   |   Flags=0     |             Length            |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                                                               |
   * \                         Padding Data                          /
   * /                                                               \
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * }}}
   */
  implicit val codec = {
    val x = "Initiation" | {
      constant(uint8.encode(0x84).require) ~>
        ignore(8) ~>
        uint16 >>:~ { length =>
          ignore(length - 4).hlist
        }
    }.dropUnits.as[PaddingChunk]

  }
}

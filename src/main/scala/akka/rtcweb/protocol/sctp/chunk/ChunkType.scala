package akka.rtcweb.protocol.sctp.chunk

import scodec.Codec
import scodec.codecs._

sealed trait ChunkType

object ChunkType {
  /** Payload Data */
  case object DATA extends ChunkType
  /** Initiation */
  case object INIT extends ChunkType
  /** Initiation Acknowledgement */
  case object `INIT ACK` extends ChunkType
  /** Selective Acknowledgement  */
  case object SACK extends ChunkType
  /** Heartbeat Request */
  case object HEARTBEAT extends ChunkType
  /** Heartbeat Acknowledgement */
  case object `HEARTBEAT ACK` extends ChunkType
  /** Abort */
  case object ABORT extends ChunkType
  /** Shutdown */
  case object SHUTDOWN extends ChunkType
  /** Shutdown Acknowledgement */
  case object `SHUTDOWN ACK` extends ChunkType
  /** Operation Error */
  case object ERROR extends ChunkType
  /** State Cookie */
  case object `COOKIE ECHO` extends ChunkType
  /** Cookie Acknowledgement */
  case object `COOKIE ACK` extends ChunkType
  /** Reserved for Explicit Congestion Notification Echo */
  case object ECNE extends ChunkType
  /** Reserved for Congestion Window Reduced */
  case object CWR extends ChunkType
  /** Shutdown Complete */
  case object `SHUTDOWN COMPLETE` extends ChunkType
  /** Available */
  case object AVAILABLE extends ChunkType
  /** reserved for IETF-defined Chunk Extensions */
  final val reserved: Set[Int] = Set(63, 127, 191, 255)

  final def isAvailable(raw: Int): Option[AVAILABLE.type] = raw match {
    case d if 15 to 62 contains d => Some(AVAILABLE)
    case d if 64 to 128 contains d => Some(AVAILABLE)
    case d if 128 to 190 contains d => Some(AVAILABLE)
    case d if 192 to 254 contains d => Some(AVAILABLE)
    case _ => None
  }

  /** size=1byte */
  implicit val codec: Codec[ChunkType] = mappedEnum(uint8,
    DATA -> 0,
    INIT -> 1,
    `INIT ACK` -> 2,
    SACK -> 3,
    HEARTBEAT -> 4,
    `HEARTBEAT ACK` -> 5,
    ABORT -> 6,
    SHUTDOWN -> 7,
    `SHUTDOWN ACK` -> 8,
    ERROR -> 9,
    `COOKIE ECHO` -> 10,
    `COOKIE ACK` -> 11,
    ECNE -> 12,
    CWR -> 13,
    `SHUTDOWN COMPLETE` -> 14
  )

}
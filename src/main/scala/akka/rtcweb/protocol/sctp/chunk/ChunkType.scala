package akka.rtcweb.protocol.sctp.chunk

import scodec.{ Err, Codec }
import scodec.codecs._
import scalaz.{ -\/, \/- }

private[sctp] sealed trait ChunkType
private[sctp] sealed trait KnownChunkType extends ChunkType

private[sctp] object ChunkType {
  /** Payload Data */
  case object DATA extends KnownChunkType
  /**
   * Initiation
   * @see [[akka.rtcweb.protocol.sctp.chunk.Initiation]]
   */
  case object INIT extends KnownChunkType
  /** Initiation Acknowledgement */
  case object `INIT ACK` extends KnownChunkType
  /** Selective Acknowledgement  */
  case object SACK extends KnownChunkType
  /** Heartbeat Request */
  case object HEARTBEAT extends KnownChunkType
  /** Heartbeat Acknowledgement */
  case object `HEARTBEAT ACK` extends KnownChunkType
  /** Abort */
  case object ABORT extends KnownChunkType
  /** Shutdown */
  case object SHUTDOWN extends KnownChunkType
  /** Shutdown Acknowledgement */
  case object `SHUTDOWN ACK` extends KnownChunkType
  /** Operation Error */
  case object ERROR extends KnownChunkType
  /** State Cookie */
  case object `COOKIE ECHO` extends KnownChunkType
  /** Cookie Acknowledgement */
  case object `COOKIE ACK` extends KnownChunkType
  /** Reserved for Explicit Congestion Notification Echo */
  case object ECNE extends KnownChunkType
  /** Reserved for Congestion Window Reduced */
  case object CWR extends KnownChunkType
  /** Shutdown Complete */
  case object `SHUTDOWN COMPLETE` extends KnownChunkType
  /** Available */
  case object AVAILABLE extends KnownChunkType

  /**
   * Re-configuration Chunk (rfc6525)
   * @see [[ReConfig]]
   * @see [[http://tools.ietf.org/html/rfc6525#section-3.1 rfc6525: RE-CONFIG Chunk]]
   */
  case object `RE-CONFIG` extends KnownChunkType

  /**
   * Forward Cumulative TSN (rfc3758)
   * @see [[ForwardCumulativeTSNChunk]]
   * @see [[https://tools.ietf.org/html/rfc3758#section-3.2 rfc3758: Forward Cumulative TSN Chunk Definition]]
   */
  case object `FORWARD TSN` extends KnownChunkType

  final case class UNKNOWN(`type`: Int) extends ChunkType
  object UNKNOWN {
    implicit val codec: Codec[UNKNOWN] = uint8.as[UNKNOWN]
  }

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
  val knownChunkTypeCodec: Codec[ChunkType] = mappedEnum(uint8,
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
    `SHUTDOWN COMPLETE` -> 14,
    `RE-CONFIG` -> 130,
    `FORWARD TSN` -> 192
  )

  implicit val codec: Codec[ChunkType] = choice(
    knownChunkTypeCodec,
    UNKNOWN.codec.widen[ChunkType](identity, {
      case g: UNKNOWN => \/-(g)
      case a => -\/(Err(s"$a is not UNKNOWN"))
    })
  )

}
package akka.rtcweb.protocol.ice

import java.net.InetSocketAddress

case class Candidate(
  transport: Transport,
  priority: Priority,
  connectionAddress: InetSocketAddress,
  port: Int)

final case class Priority(priority: Long) extends AnyVal

sealed trait Transport
object Transport {
  final case class UnknownTransportExtension(token: String) extends Transport
  case object UDP extends Transport
}

sealed trait CandidateType

object CandidateType {

  final case class UnknownCandidateType(token: String) extends CandidateType
  case object host extends CandidateType
  case object srflx extends CandidateType
  case object prflx extends CandidateType
  case object relay extends CandidateType
}


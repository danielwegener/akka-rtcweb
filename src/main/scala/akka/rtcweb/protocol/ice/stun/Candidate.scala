package akka.rtcweb.protocol.ice.stun

import java.net.InetSocketAddress

sealed trait Candidate { def address: InetSocketAddress; def base: HostCandidate }

final case class HostCandidate(address: InetSocketAddress) extends Candidate { override def base: HostCandidate = this }

final case class ServerReflexiveCandidate(address: InetSocketAddress, base: HostCandidate) extends Candidate

final case class PeerReflexiveCandidate(address: InetSocketAddress, base: HostCandidate) extends Candidate
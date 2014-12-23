package akka.rtcweb.protocol.ice.stun

import java.net.InetSocketAddress

sealed trait Candidate { def socketAddress: InetSocketAddress }

case class HostCandidate(socketAddress: InetSocketAddress) extends Candidate
case class ServerReflexiveCandidate(socketAddress: InetSocketAddress) extends Candidate
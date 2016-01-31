package akka.rtcweb.protocol.ice

import java.net.{ InetAddress, InetSocketAddress }
import java.util.UUID

import akka.actor._
import akka.io.{ IO, Udp }
import akka.rtcweb.protocol.ice.IceAgent._
import akka.rtcweb.protocol.ice.stun.StunMessage.TransactionId
import akka.rtcweb.protocol.ice.stun._
import akka.rtcweb.protocol.jsep.RTCPeerConnection.StunServerDescription
import akka.rtcweb.protocol.scodec.Decoded
import akka.stream.io.InterfaceMonitor.DeltaResponse
import akka.stream.io.InterfaceMonitor
import akka.util.ByteString
import scodec.bits.ByteVector
import scala.concurrent.duration._

import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.language.postfixOps

object IceAgent {

  def props(role: AgentRole, listener: ActorRef, stunServers: Vector[StunServerDescription], portRange: Range) = Props(new IceAgent(role, listener, stunServers, portRange))

  final case class OnIceCandidate(candidates: Candidate)
  final case class OnIceGatheringStateChange(state: IceGatheringState)

  case object CreateOffer
  case class CreateOfferResponse(stunCandidates: Vector[Candidate])

  sealed trait AgentRole
  object GatherCandidates
  object AgentRole {
    case object Controlling extends AgentRole
    case object Controlled extends AgentRole
  }

}

class IceAgent private[ice] (val agentRole: AgentRole, val listener: ActorRef, val iceServers: Vector[StunServerDescription], val portRange: Range) extends Actor with ActorLogging {

  val ta = 500 milliseconds

  val interfaceMonitor = context.actorOf(InterfaceMonitor.props(5 seconds), "interface-monitor")
  //interfaceMonitor ! InterfaceMonitor.Register(self)
  interfaceMonitor ! InterfaceMonitor.Delta(Set.empty)

  var gatheringState: IceGatheringState = IceGatheringState.`new`

  implicit val executionContext = context.system.dispatcher
  val udpExt = IO(Udp)(context.system)

  override def receive: Receive = collectingInterfaces()

  def sanitizeHostname(hostname: String) = hostname.replace('/', ';').replace(':', '.').replace('%', ';')

  var stunAgentCounter = 0

  def collectingInterfaces(): Receive = {
    case DeltaResponse(up, _) =>
      val ifs = up.map(interface => new InetSocketAddress(interface.address, 0)).toSet
      ifs.foreach { isa =>
        val stunAgent = context.actorOf(StunAgent.props(self), s"stun-agent-${sanitizeHostname(isa.getHostString)}-$stunAgentCounter")
        stunAgentCounter += 1
        udpExt.tell(Udp.Bind(stunAgent, isa), stunAgent)
      }

      context.become(awaitInterfacesBound(ifs.map(_.getAddress), Map.empty))
      listener ! OnIceGatheringStateChange(IceGatheringState.gathering)

  }

  case class StunAgentBinding(address: InetSocketAddress, actorRef: ActorRef)

  def awaitInterfacesBound(waitingFor: Set[InetAddress], available: Map[StunAgentBinding, ByteVector]): Receive = {
    case Udp.Bound(localAddress) if waitingFor == Set(localAddress.getAddress) =>
      log.debug(s"Bound interface [$localAddress].")
      val all = available + (StunAgentBinding(localAddress, sender()) -> mkTransactionId())
      log.info("All interfaces bound. Start gathering now.")

      sendCandidateChecks(all)
      context.become(gathering(all, Map.empty))

    case Udp.Bound(localAddress) =>
      log.info(s"Bound interface $localAddress.")
      context.become(awaitInterfacesBound(waitingFor - localAddress.getAddress, available + (StunAgentBinding(localAddress, sender()) -> mkTransactionId())))
  }

  def sendCandidateChecks(udpSockets: Map[StunAgentBinding, TransactionId]) = {
    for {
      (StunAgentBinding(localSocketAddress, socketRef), transactionId) <- udpSockets.toVector
      iceServer <- iceServers

      stunBindingRequest = StunMessage(stun.Class.request, stun.Method.Binding, transactionId)
    } {
      socketRef ! StunAgent.SendStunMessage(stunBindingRequest, iceServer.address)
    }
    context.system.scheduler.scheduleOnce(2 seconds, self, GatheringTimeout)
  }

  implicit val codec = StunMessage.codec
  case object GatheringTimeout

  def gathering(waitingForStunBindingResponse: Map[StunAgentBinding, ByteVector], serverReflexiveCandidates: Map[StunAgentBinding, ServerReflexiveCandidate]): Receive = {

    case StunAgent.ReceivedStunMessage(StunMessage(Class.successResponse, Method.Binding, transactionId, Vector(`XOR-MAPPED-ADDRESS`(family, reflexivePort, reflexiveAddress))), _) if waitingForStunBindingResponse.exists { case (StunAgentBinding(_, senderRef), tid) if senderRef == sender && tid == transactionId => true; case _ => false } =>
      val udpBinding = waitingForStunBindingResponse.keys.find(_.actorRef == sender).get
      val reflexiveSocketAddress = new InetSocketAddress(reflexiveAddress, reflexivePort)
      val srvfx = ServerReflexiveCandidate(reflexiveSocketAddress, HostCandidate(udpBinding.address))
      log.info(s"received a valid server-reflexive binding: $srvfx")

      context.become(gathering(waitingForStunBindingResponse - udpBinding, serverReflexiveCandidates + (udpBinding -> srvfx)))

    case GatheringTimeout =>
      val hostCandidatesWithoutReflexive = waitingForStunBindingResponse.keys.map(k => (k, (HostCandidate(k.address), None))).toMap

      val reflexives = serverReflexiveCandidates
        .filterNot { case (_, srv) => srv.base.address == srv.address }
        .mapValues(rc => (rc.base, Some(rc)))

      val combined = reflexives ++ hostCandidatesWithoutReflexive

      context.become(ready(combined, Vector.empty))

      serverReflexiveCandidates.values.foreach(reflexiveCandidate => listener ! OnIceCandidate(stunToIceCandidate(reflexiveCandidate)))

      listener ! OnIceGatheringStateChange(IceGatheringState.complete)
      log.info(s"become ready with $combined")

  }

  case class PendingOffer(iceUfrag: IceUfrag, icePwd: IcePwd)

  def ready(bindings: Map[StunAgentBinding, (HostCandidate, Option[ServerReflexiveCandidate])], offers: Vector[PendingOffer]): Receive = {
    case CreateOffer =>
      val allCandidates: Vector[stun.Candidate] = bindings.values.toVector.flatMap { case (h, f) => Vector(h) ++: f.toVector }
      sender() ! CreateOfferResponse(allCandidates.map(stunToIceCandidate))

  }

  private def mkTransactionId(): TransactionId = {
    val buffer = new Array[Byte](12)
    ThreadLocalRandom.current().nextBytes(buffer)
    ByteVector(buffer)
  }

  private def mkRandomIce(): String = {
    //todo: conformance to RFC 5245 15.4.
    UUID.randomUUID().toString.replace("-", "")
  }

  private def stunToIceCandidate(candidate: stun.Candidate): Candidate = {
    val candidateType = candidate match {
      case _: HostCandidate => CandidateType.HostCandidate
      case _: PeerReflexiveCandidate => CandidateType.PeerReflexiveCandidate
      case _: ServerReflexiveCandidate => CandidateType.ServerReflexiveCandidate
    }
    val relayedConnectionAddress = candidate match {
      case _: HostCandidate => None
      case c => Some(c.base.address)
    }
    Candidate(
      computeFoundation(candidate),
      1, Transport.UDP,
      computePriority(candidate),
      candidate.address,
      candidateType,
      relayedConnectionAddress,
      Vector[Candidate.ExtensionAttribute](
        ("generation", "0"))
    )
  }

  private def computePriority(candidate: stun.Candidate): Priority = {
    log.warning("IceAgent.computePriority :implementation is missing! :(((")
    Priority(0L)
  }

  private def computeFoundation(candidate: stun.Candidate): String = {
    "udp" +
      candidate.base.address.getAddress.getHostAddress +
      "ip_of_stun_server"
  }

}

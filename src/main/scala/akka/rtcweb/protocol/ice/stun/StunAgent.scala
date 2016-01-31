package akka.rtcweb.protocol.ice.stun

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Udp
import akka.rtcweb.protocol.ice.stun.StunMessage._
import akka.rtcweb.protocol.ice.stun.StunAgent.{ SendStunMessage, ReceivedStunMessage }
import akka.rtcweb.protocol.scodec.{ Encoded, Decoded }

import scala.concurrent.duration._
import scala.language.postfixOps

object StunAgent {

  sealed trait Message

  sealed trait Event extends Message

  sealed trait Command extends Message

  case class ReceivedStunMessage(message: StunMessage, sender: InetSocketAddress) extends Event

  case class SendStunMessage(stunMessage: StunMessage, target: InetSocketAddress) extends Command

  /**
   *
   * @param retransmissionCount          rc
   * @param initialRetransmissionTimeout rto
   * @return
   */
  def props(handler: ActorRef, retransmissionCount: Int = 7, initialRetransmissionTimeout: FiniteDuration = 500 millis) =
    Props[StunAgent](new StunAgent(handler, retransmissionCount, initialRetransmissionTimeout))

}

/**
 * Wrapper for stun over udp retransmission behaviour rfc5389 section 7.2.1.
 * It acts as a proxy for an UDP channel obtained by UDP.Bind.
 *
 * It accepts Udp.Send messages and emits Udp.Received messages.
 * It accepts StunAgent.SendStunMessage emits StunAgent.ReceivedStunMessage.
 *
 *
 */
class StunAgent private (val handler: ActorRef, val retransmissionCount: Int, val initialRetransmissionTimeout: FiniteDuration) extends Actor with ActorLogging {

  case class RetransmissionTimeout(transactionId: TransactionId)

  case class ResendEntry(message: StunMessage, retransmissionsLeft: Int, retransmissionTimeout: FiniteDuration, scheduledRetransmission: Cancellable)

  var retransmissionCache: Map[TransactionId, ResendEntry] = Map.empty

  val software = SOFTWARE("akka-rtcweb")
  val unknownAttributeErrorCode = `ERROR-CODE`(`ERROR-CODE`.Code.`Unknown Attribute`, "Unknown Attribute")

  val scheduler = context.system.scheduler
  implicit val executionContext = context.dispatcher

  override def receive: Actor.Receive = {
    case bound @ Udp.Bound(localAddress) =>
      handler ! bound
      context.become(ready(sender()))
      log.debug("stun agent bound to {}", sender())
  }

  def ready(udpChannel: ActorRef): Actor.Receive = {

    // its a stun message from the wire
    case Udp.Received(Decoded(message: StunMessage), senderAddress) => message match {

      // its a stun message with unknown mandatory attributes
      case sm @ StunMessage(Class.request, method, transactionId, attributes) if attributes.map(_.attributeType).collect { case u: StunAttributeType.UNKNOWN if u.mustUnderstand => u }.nonEmpty =>
        // Reject stun messages with unknown attributes
        val unknownsAttributeCodes = attributes.map(_.attributeType)
          .collect { case u: StunAttributeType.UNKNOWN if u.mustUnderstand => u }

        val errorAttributes = Vector(unknownAttributeErrorCode,
          `UNKNOWN-ATTRIBUTES`(unknownsAttributeCodes),
          software)

        udpChannel ! Udp.Send(Encoded(StunMessage(Class.errorResponse, method, transactionId, errorAttributes)), senderAddress)
        log.info("rejected a stun message with unsupported attributes {}: {}", unknownsAttributeCodes.mkString(","), sm)

      //TODO: do fingerprint and authentication checks

      // its a valid stun request message
      case sm @ StunMessage(Class.request, method, transactionId, _) =>
        // confirm and escalate stun messages
        udpChannel ! Udp.Send(Encoded(StunMessage(Class.successResponse, method, transactionId, Vector(software))), senderAddress)
        handler ! ReceivedStunMessage(sm, senderAddress)
        log.debug("accepted and acknowledged a stun request message: {}", sm)

      // its a stun response message from the wire
      case sm @ StunMessage(r @ (Class.successResponse | Class.errorResponse), method, transactionId, attributes) =>
        if (retransmissionCache.contains(transactionId)) {
          // unschedule retransmission
          retransmissionCache.get(transactionId).foreach(_.scheduledRetransmission.cancel())
          retransmissionCache -= transactionId
          handler ! ReceivedStunMessage(sm, senderAddress)
          log.debug("received an expected response and unscheduled retransmission: {}", sm)
        } else {
          log.debug("received an ignored unexpected response (could be a retransmission): {}", sm)
        }
    }

    case other: Udp.Received =>
      handler ! other
      log.debug("received a payload datagram ({}bytes) and delegated it to client.", other.data.size)

    case sendBinary: Udp.Send =>
      udpChannel ! sendBinary
      log.debug("sent a payload datagram ({}bytes).", sendBinary.payload.size)

    // its a stun indication message from the client
    case SendStunMessage(stunMessage @ StunMessage(Class.indication, _, _, _), target) =>
      // just push it to the wire
      udpChannel ! Udp.Send(Encoded(stunMessage), target)
      log.debug("sent a stun indication message: {}", stunMessage)

    // its a stun request message from the client
    case SendStunMessage(stunMessage @ StunMessage(Class.request, _, transactionId, _), target) =>
      // push it to the wire and schedule retransmission
      udpChannel ! Udp.Send(Encoded(stunMessage), target)
      val scheduledRetransmission = scheduler.scheduleOnce(initialRetransmissionTimeout, self, RetransmissionTimeout(transactionId))
      val resendEntry = ResendEntry(stunMessage, retransmissionCount, initialRetransmissionTimeout, scheduledRetransmission)
      retransmissionCache += transactionId -> resendEntry
      log.debug("sent a stun request to [{}] and scheduled retransmission: [{}]", target, stunMessage)

    case SendStunMessage(sm @ StunMessage(Class.errorResponse | Class.successResponse, _, _, _), _) =>
      log.error("Client attempts to send a response message.")

    // its our retransmission timer
    case rt @ RetransmissionTimeout(transactionId) =>
      //log.debug("attempt to retransmit {}", transactionId)
      retransmissionCache.get(transactionId).foreach {
        case ResendEntry(message, retransmissionsLeft, retransmissionTimeout, scheduledRetransmission) =>
          scheduledRetransmission.cancel()
          if (retransmissionsLeft > 0) {
            val newRetransmissionsLeft = retransmissionsLeft - 1
            val newRetransmissionTimeout = retransmissionTimeout * 2
            val newScheduledRetransmission = scheduler.scheduleOnce(newRetransmissionTimeout, self, rt)
            retransmissionCache += transactionId -> ResendEntry(message, newRetransmissionsLeft, newRetransmissionTimeout, newScheduledRetransmission)
          } else {
            log.debug("retransmission failed after {} retries: {}", retransmissionCount, transactionId)
          }
      }
  }

  override def postStop(): Unit = {
    // cancel all retransmissions
    retransmissionCache.values.foreach(_.scheduledRetransmission.cancel())
  }
}
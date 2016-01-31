package akka.rtcweb

import akka.actor._
import akka.io.IO
import akka.rtcweb.RtcWebManager.CreateTransportAgent

object RtcWeb extends ExtensionId[RtcWebExt] with ExtensionIdProvider {

  override def lookup = RtcWeb
  override def createExtension(system: ExtendedActorSystem): RtcWebExt = new RtcWebExt(system)
  override def get(system: ActorSystem): RtcWebExt = super.get(system)

}

/**
 * INTERNAL API
 */
private[rtcweb] class RtcWebExt(system: ExtendedActorSystem) extends IO.Extension {
  val manager: ActorRef = system.systemActorOf(Props[RtcWebManager], name = "IO-RTCWEB")
}

object RtcWebManager {
  sealed trait Message
  case object CreateTransportAgent

}

/**
 * INTERNAL API
 */
private[rtcweb] class RtcWebManager extends Actor {

  override def receive: Receive = {
    case CreateTransportAgent =>

  }
}
package akka.rtcweb

import akka.actor._
import akka.io.IO


object RtcWeb extends ExtensionId[RtcWebExt] with ExtensionIdProvider {

  override def lookup = RtcWeb
  override def createExtension(system: ExtendedActorSystem): RtcWebExt = new RtcWebExt(system)
  override def get(system: ActorSystem): RtcWebExt = super.get(system)


}

/**
 * INTERNAL API
 */
private[rtcweb] class RtcWebExt(system: ExtendedActorSystem) extends IO.Extension {
  val manager: ActorRef = system.systemActorOf(Props[RtcWebManager], name = "IO-UDP-STREAM")
}


/**
 * INTERNAL API
 */
private [rtcweb] class RtcWebManager extends Actor {




}
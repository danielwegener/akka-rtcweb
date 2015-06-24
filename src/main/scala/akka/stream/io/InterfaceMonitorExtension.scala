package akka.stream.io

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

object InterfaceMonitorExtension extends ExtensionId[InterfaceMonitorExtensionImpl] with ExtensionIdProvider {

  override def lookup() = InterfaceMonitorExtension

  override def createExtension(system: ExtendedActorSystem): InterfaceMonitorExtensionImpl = {
    new InterfaceMonitorExtensionImpl(system, 1 second)
  }

}

class InterfaceMonitorExtensionImpl(system: ExtendedActorSystem, interval:FiniteDuration) extends Extension {
  private val ref  = system.systemActorOf(InterfaceMonitor.props(interval), "InterfaceMonitor")
  def register(implicit ctx:ActorContext) = ref.tell(InterfaceMonitor.Register,ctx.self)
}

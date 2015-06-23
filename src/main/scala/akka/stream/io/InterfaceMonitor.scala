package akka.stream.io

import java.net.{ InetAddress, NetworkInterface }

import akka.actor._
import akka.stream.io.InterfaceMonitor._
import akka.util.ByteString

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.JavaConverters._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.reflect.ClassTag

object InterfaceMonitor {

  def props(interval: FiniteDuration): Props = Props.apply(new InterfaceMonitor(interval))

  sealed trait InterfaceMonitorApi

  sealed trait InterfaceStateEvent

  final case class Register(listener: ActorRef) extends InterfaceMonitorApi

  final case class UnRegister(listener: ActorRef) extends InterfaceMonitorApi

  final case class NetworkInterfaceRepr(name: String, hardwareAddress: ByteString, address: InetAddress, prefixLength: Short)

  final case class InterfaceStateChanges(changes: Seq[InterfaceStateEvent])

  final case class IfUp(interface: NetworkInterfaceRepr) extends InterfaceStateEvent
  final case class IfDown(interface: NetworkInterfaceRepr) extends InterfaceStateEvent

}

class InterfaceMonitor private[io] (interval: FiniteDuration) extends Actor with ActorLogging {

  import context.dispatcher
  val tick = context.system.scheduler.schedule(Duration.Zero, interval, self, Tick)
  var knownInterfaces: Set[NetworkInterfaceRepr] = Set.empty
  var listeners: Set[ActorRef] = Set.empty

  def specialized[T: ClassTag](f: PartialFunction[T, Unit]): PartialFunction[Any, Unit] = {
    case t: T if f.isDefinedAt(t) => f(t)
    case _ =>
  }

  def fallback(f: PartialFunction[Any, Unit]): PartialFunction[Any, Unit] = f

  override def receive: Receive = {
    case Register(listener) =>
      context.watch(listener)
      listeners += listener
    case UnRegister(listener) =>
      context.unwatch(listener)
      listeners -= listener
    case Terminated(listener) => listeners -= listener
    case Tick => onTick()
    case _ =>
  }

  def onTick() = {
    val currentInterfaces = enumerationAsScalaIterator(NetworkInterface.getNetworkInterfaces).toList
      .flatMap(f => f :: enumerationAsScalaIterator(f.getSubInterfaces).toList)
      .filter(_.isUp)
      .flatMap(i => i.getInterfaceAddresses.asScala.map(a => (i, a)))
      .map { case (i, a) => NetworkInterfaceRepr(i.getName, Option(i.getHardwareAddress).map(ByteString.apply).getOrElse(ByteString.empty), a.getAddress, a.getNetworkPrefixLength) }.toSet
    val upgoingInterfaces = currentInterfaces -- knownInterfaces
    val downgoingInterfaces = knownInterfaces -- currentInterfaces

    val changes = InterfaceStateChanges(upgoingInterfaces.toSeq.map(IfUp.apply) ++ downgoingInterfaces.toSeq.map(IfDown.apply))
    listeners.foreach(_ ! changes)
    changes.changes.foreach { change => log.debug(s"Interface state change: $change") }
    knownInterfaces = currentInterfaces
  }

  override def postStop(): Unit = tick.cancel()

  object Tick
}

/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.io

import akka.actor._
import akka.io.Udp._
import akka.io.{ IO, Udp }
import akka.stream.MaterializerSettings
import akka.stream.impl._
import akka.stream.io.StreamUdp.{ UdpConnection, UdpPacket }
import akka.stream.io.StreamUdpManager.ExposedProcessor
import akka.stream.io.UdpActor.{ UdpStreamException, WriteAck }
import org.reactivestreams.Processor

import scala.util.control.NoStackTrace

/**
 * INTERNAL API
 */
private[akka] object UdpActor {
  case object WriteAck extends Udp.Event
  class UdpStreamException(msg: String) extends RuntimeException(msg) with NoStackTrace

  def props(bindCmd: Udp.Bind, requester: ActorRef, settings: MaterializerSettings): Props =
    Props(new UdpActor(bindCmd, requester, settings)).withDispatcher(settings.dispatcher)
}

/**
 * INTERNAL API
 */
private[akka] class UdpActor(bindCmd: Udp.Bind, requester: ActorRef, _settings: MaterializerSettings) extends UdpStreamActor(_settings) {
  import context.system

  val initSteps = new SubReceive(waitingExposedProcessor)

  override def receive = initSteps orElse super.receive

  def waitingExposedProcessor: Receive = {
    case ExposedProcessor(processor) ⇒
      IO(Udp) ! bindCmd.copy(handler = self)
      initSteps.become(waitingBound(processor))
  }

  def waitingBound(processor: Processor[UdpPacket, UdpPacket]): Receive = {
    case Bound(localAddress) ⇒
      val connection = sender()
      udpInputs.setConnection(connection)
      udpOutputs.setConnection(connection)
      requester ! UdpConnection(localAddress, processor, processor)
      initSteps.become(Actor.emptyBehavior)
    case f: CommandFailed ⇒
      val ex = new UdpStreamException("Connection failed.")
      requester ! Status.Failure(ex)
      fail(ex)
  }
}

/**
 * INTERNAL API
 */
private[akka] abstract class UdpStreamActor(val settings: MaterializerSettings) extends Actor {
  val primaryInputs: Inputs = new BatchingInputBuffer(settings.initialInputBufferSize, writePump) {
    override def inputOnError(e: Throwable): Unit = fail(e)
  }

  val primaryOutputs: Outputs =
    new FanoutOutputs(settings.maxFanOutBufferSize, settings.initialFanOutBufferSize, self, readPump) {
      override def afterShutdown(): Unit = {
        udpInputs.cancel()
        UdpStreamActor.this.tryShutdown()
      }
    }

  object udpInputs extends DefaultInputTransferStates {
    private var closed: Boolean = false
    private var pendingElement: UdpPacket = null
    private var connection: ActorRef = _

    val subreceive = new SubReceive(Actor.emptyBehavior)

    def setConnection(c: ActorRef): Unit = {
      connection = c
      // Prefetch
      c ! ResumeReading
      subreceive.become(handleRead)
      readPump.pump()
    }

    def handleRead: Receive = {
      case Received(data, sender) ⇒
        pendingElement = UdpPacket(data, sender)
        readPump.pump()
      case Unbound ⇒
        closed = true
        udpOutputs.complete()
        writePump.pump()
        readPump.pump()
      case CommandFailed(cmd) ⇒ fail(new UdpStreamException(s"Udp command [$cmd] failed"))
    }

    override def inputsAvailable: Boolean = pendingElement ne null
    override def inputsDepleted: Boolean = closed && !inputsAvailable
    override def isClosed: Boolean = closed

    override def cancel(): Unit = {
      closed = true
      pendingElement = null
    }

    override def dequeueInputElement(): Any = {
      val elem = pendingElement
      pendingElement = null
      connection ! ResumeReading
      elem
    }
  }

  object udpOutputs extends DefaultOutputTransferStates {
    private var closed: Boolean = false
    private var pendingDemand = true
    private var connection: ActorRef = _

    private def initialized: Boolean = connection ne null

    def setConnection(c: ActorRef): Unit = {
      connection = c
      writePump.pump()
      subreceive.become(handleWrite)
    }

    val subreceive = new SubReceive(Actor.emptyBehavior)

    def handleWrite: Receive = {
      case WriteAck ⇒
        pendingDemand = true
        writePump.pump()

    }

    override def isClosed: Boolean = closed
    override def cancel(e: Throwable): Unit = {
      closed = true
    }
    override def complete(): Unit = {
      closed = true
    }
    override def enqueueOutputElement(elem: Any): Unit = {
      val msg = elem.asInstanceOf[UdpPacket]
      connection ! Send(msg.content, msg.address, WriteAck)
      pendingDemand = false
    }

    override def demandAvailable: Boolean = pendingDemand
  }

  object writePump extends Pump {

    def running = TransferPhase(primaryInputs.NeedsInput && udpOutputs.NeedsDemand) { () ⇒
      while (primaryInputs.inputsAvailable)
        udpOutputs.enqueueOutputElement(primaryInputs.dequeueInputElement().asInstanceOf[UdpPacket])
    }

    override protected def pumpFinished(): Unit = {
      udpOutputs.complete()
      tryShutdown()
    }
    override protected def pumpFailed(e: Throwable): Unit = fail(e)
    override protected def pumpContext: ActorRefFactory = context
  }

  object readPump extends Pump {

    def running = TransferPhase(udpInputs.NeedsInput && primaryOutputs.NeedsDemand) { () ⇒
      primaryOutputs.enqueueOutputElement(udpInputs.dequeueInputElement())
    }

    override protected def pumpFinished(): Unit = {
      primaryOutputs.complete()
      tryShutdown()
    }
    override protected def pumpFailed(e: Throwable): Unit = fail(e)
    override protected def pumpContext: ActorRefFactory = context
  }

  override def receive =
    primaryInputs.subreceive orElse primaryOutputs.subreceive orElse udpInputs.subreceive orElse udpOutputs.subreceive

  readPump.nextPhase(readPump.running)
  writePump.nextPhase(writePump.running)

  def fail(e: Throwable): Unit = {
    udpInputs.cancel()
    udpOutputs.cancel(e)
    primaryInputs.cancel()
    primaryOutputs.cancel(e)
  }

  def tryShutdown(): Unit = if (primaryInputs.isClosed && udpInputs.isClosed && udpOutputs.isClosed) context.stop(self)

}
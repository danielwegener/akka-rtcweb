package akka.rtcweb

import java.net.InetSocketAddress

import akka.actor._
import akka.io._
import akka.util.ByteString
import collection.immutable.Seq

object UdpMuxer {
  def props(receivers: List[(ByteString => Boolean, ActorRef)]) = Props(new UdpMuxer(receivers))
}

class UdpMuxer private (private val receivers: Seq[(ByteString => Boolean, ActorRef)]) extends Actor {

  override def receive = {
    case bound @ Udp.Bound(localAddress) =>
      context.become(ready(sender(), localAddress))
      receivers.foreach(_._2 ! bound)
  }

  def ready(socket: ActorRef, localAddress: InetSocketAddress): Receive = {
    case received @ Udp.Received(data, _) =>
      receivers.find(_._1(data)).foreach(_._2 ! received)
    case send @ Udp.Send(data, remote, ack) => socket ! Udp.Send(data, remote, ack)
    case Udp.Unbind => socket ! Udp.Unbind
    case unbound @ Udp.Unbound =>
      receivers.foreach(_._2 ! unbound)
      context.stop(self)
  }

}
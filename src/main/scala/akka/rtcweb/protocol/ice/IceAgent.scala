package akka.rtcweb.protocol.ice

import java.net.NetworkInterface

import scala.collection.JavaConversions._

import akka.actor.{ ActorLogging, Actor }

object IceAgent {
}

class IceAgent() extends Actor with ActorLogging {

  override def receive: Receive = ???

  def findHostCandidates = {
    val localInterfaces = NetworkInterface.getNetworkInterfaces.toSeq
    val localAddresses = localInterfaces.flatMap(_.getInetAddresses.toSeq)

  }

}

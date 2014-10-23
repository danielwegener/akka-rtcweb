package akka.rtcweb.protocol.sdp.negotiation

import akka.rtcweb.protocol.sdp.{PropertyAttribute, ValueAttribute, Attribute, SessionDescription}

object Offer {

  val highestInitialVersionId:Long = 67108863

  /** If the offerer wishes to only receive media
   * from its peer, it MUST mark the stream as recvonly.
   */
  def isRecvOnly(attr:Attribute):Boolean = attr match {
    case PropertyAttribute("recvonly") => true
    case _ => false
  }

  /**  If the offerer wishes to only send media on a stream to its peer, it
   * MUST mark the stream as sendonly with the "a=sendonly" attribute.
   */
  def isSendOnly(attr:Attribute):Boolean = attr match {
    case PropertyAttribute("sendonly") => true
    case _ => false
  }

}

case class Offer(sessionDescription: SessionDescription) {
  //require(sessionDescription)
}

case class Response(sessionDescription:SessionDescription) {

}
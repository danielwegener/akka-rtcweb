package akka.rtcweb.protocol

import akka.stream.scaladsl.Flow
import akka.util.ByteString

/**
 * This is an akka adoption of the  Javascript Session Establishment Protocol
 * [draft-ietf-rtcweb-jsep-09](http://tools.ietf.org/html/draft-ietf-rtcweb-jsep-09)
 *
 * Let [[jsep.RTCPeerConnection]] be your entry point and friend.
 *
 */
package object jsep {

  /**
   * A flow from ByteStrings or Strings to ByteStrings or Strings
   */
  type DataChannelFlow = Flow[Either[ByteString, String], Either[ByteString, String], _]
  type DataChannelMessage = Either[ByteString, String]
  type BinaryMessageType = Left[ByteString, _]
  type StringMessageType = Right[_, String]

}

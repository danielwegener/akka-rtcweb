package akka.rtcweb.protocol.sctp

import scodec._
import scodec.bits._
import codecs._

case class SctpPacket(header:SctpCommonHeader)

case object SctpPacket {
  implicit val codec =
    ("common_header" | SctpCommonHeader.codec)  //::
    }.as[Port]
  }
}


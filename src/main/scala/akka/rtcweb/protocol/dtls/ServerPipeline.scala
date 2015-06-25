package akka.rtcweb.protocol.dtls

import akka.event.LoggingAdapter
import akka.rtcweb.protocol.dtls.StreamDtls.{ DtlsConnection, DtlsRecord }
import akka.stream.io.StreamUdp
import akka.stream.io.StreamUdp.{ UdpConnection, UdpPacket }
import akka.stream.{ Materializer, TransformerLike }

trait DtlsRecordParser extends TransformerLike[UdpPacket, DtlsRecord]

class ServerPipeline(serverSettings: ServerSettings, materializer: Materializer, log: LoggingAdapter)
    extends (StreamUdp.UdpConnection ⇒ StreamDtls.DtlsConnection) {

  val recordEncoder = new RecordEncoder(serverSettings)
  val recordDecoder = new RecordDecoder(serverSettings)

  override def apply(udpConnection: UdpConnection): DtlsConnection = {

    //val inputStream = Flow(udpConnection.inputStream).transform(recordDecoder)

    //val outputStream =

    ???
  }
}

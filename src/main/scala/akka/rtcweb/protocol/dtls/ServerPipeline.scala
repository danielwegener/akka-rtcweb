package akka.rtcweb.protocol.dtls

import akka.event.LoggingAdapter
import akka.rtcweb.protocol.dtls.StreamDtls.{ DtlsRecord, DtlsConnection }
import akka.stream.{ TransformerLike, FlowMaterializer }
import akka.stream.io.StreamUdp.{ UdpPacket, UdpConnection }
import akka.stream.io.StreamUdp

trait DtlsRecordParser extends TransformerLike[UdpPacket, DtlsRecord]

class ServerPipeline(serverSettings: ServerSettings, materializer: FlowMaterializer, log: LoggingAdapter)
    extends (StreamUdp.UdpConnection ⇒ StreamDtls.DtlsConnection) {

  val recordEncoder = new RecordEncoder(serverSettings)
  val recordDecoder = new RecordDecoder(serverSettings)

  override def apply(udpConnection: UdpConnection): DtlsConnection = {

    //val inputStream = Flow(udpConnection.inputStream).transform(recordDecoder)

    //val outputStream =

    ???
  }
}

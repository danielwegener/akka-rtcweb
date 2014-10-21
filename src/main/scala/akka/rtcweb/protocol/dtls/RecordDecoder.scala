package akka.rtcweb.protocol.dtls

import akka.rtcweb.protocol.dtls.StreamDtls.DtlsRecord
import akka.rtcweb.protocol.dtls.record.DtlsPlaintext
import akka.stream.Transformer
import akka.stream.io2.StreamUdp.UdpPacket
import akka.util.ByteString

import scodec._
import scodec.bits._
import scodec.codecs._

import scalaz._

import scala.collection.immutable

private[dtls] class RecordDecoder(val serverSettings: ServerSettings) extends Transformer[UdpPacket, DtlsRecord] {

  /**
   * Invoked for each element to produce a (possibly empty) sequence of
   * output elements.
   */
  override def onNext(element: UdpPacket): immutable.Seq[DtlsRecord] = {
    val raw = BitVector.apply(element.content.asByteBuffer)
    val plaintext: DtlsPlaintext = DtlsPlaintext.codec.decodeValidValue(raw)
    immutable.Seq(DtlsRecord(plaintext, element.address))
    //todo: decode more than one element
  }

}

private[dtls] class RecordEncoder(val serverSettings: ServerSettings) extends Transformer[DtlsRecord, UdpPacket] {
  /**
   * Invoked for each element to produce a (possibly empty) sequence of
   * output elements.
   */
  override def onNext(element: DtlsRecord): immutable.Seq[UdpPacket] = {
    val raw: BitVector = DtlsPlaintext.codec.encodeValid(element.content)
    List(UdpPacket(ByteString(raw.toByteBuffer), element.remoteAddress))
  }

}

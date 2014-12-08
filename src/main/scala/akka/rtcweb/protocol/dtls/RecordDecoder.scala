package akka.rtcweb.protocol.dtls

import akka.rtcweb.protocol.dtls.StreamDtls.DtlsRecord
import akka.rtcweb.protocol.dtls.record.DtlsPlaintext
import akka.stream.{TransformerLike}
import akka.stream.io.StreamUdp.UdpPacket
import akka.util.ByteString

import scodec._
import scodec.bits._
import scodec.codecs._

import scalaz._

import scala.collection.immutable

private[dtls] class RecordDecoder(val serverSettings: ServerSettings) extends TransformerLike[UdpPacket, DtlsRecord] {

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

private[dtls] class RecordEncoder(val serverSettings: ServerSettings) extends TransformerLike[DtlsRecord, UdpPacket] {
  /**
   * Invoked for each element to produce a (possibly empty) sequence of
   * output elements.
   */
  override def onNext(element: DtlsRecord): immutable.Seq[UdpPacket] = {
    val raw: BitVector = DtlsPlaintext.codec.encodeValid(element.content)
    List(UdpPacket(ByteString(raw.toByteBuffer), element.remoteAddress))
  }

}

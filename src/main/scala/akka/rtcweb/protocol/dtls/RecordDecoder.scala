package akka.rtcweb.protocol.dtls

import akka.rtcweb.protocol.dtls.StreamDtls.DtlsRecord
import akka.rtcweb.protocol.dtls.record.DtlsPlaintext
import akka.stream.TransformerLike
import akka.util.ByteString
import scodec.bits._

import scala.collection.immutable

private[dtls] class RecordDecoder(val serverSettings: ServerSettings) extends TransformerLike[ByteString, DtlsRecord] {

  /**
   * Invoked for each element to produce a (possibly empty) sequence of
   * output elements.
   */
  override def onNext(element: ByteString): immutable.Seq[DtlsRecord] = {
    val raw = BitVector(element.asByteBuffer)
    val plaintext: DtlsPlaintext = DtlsPlaintext.codec.decode(raw).require.value
    immutable.Seq(DtlsRecord(plaintext, ???))
    //todo: decode more than one element
  }

}

private[dtls] class RecordEncoder(val serverSettings: ServerSettings) extends TransformerLike[DtlsRecord, ByteString] {
  /**
   * Invoked for each element to produce a (possibly empty) sequence of
   * output elements.
   */
  override def onNext(element: DtlsRecord): immutable.Seq[ByteString] = {
    val raw: BitVector = DtlsPlaintext.codec.encode(element.content).require
    Vector(ByteString(raw.toByteBuffer))
  }

}

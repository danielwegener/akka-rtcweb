package akka.rtcweb.protocol.scodec

import akka.util.ByteString
import scodec.{ DecodeResult, Attempt, Codec }
import scodec.bits.BitVector

object Decoded {

  def unapply[T](bitVector: ByteString)(implicit codec: Codec[T]): Option[T] = {
    codec.decode(BitVector(bitVector.asByteBuffer)) match {
      case Attempt.Successful(DecodeResult(v, remainder)) if remainder.isEmpty => Some(v)
      case Attempt.Failure(e) => None
    }
  }

}

object Encoded {
  def apply[T](t: T)(implicit codec: Codec[T]): ByteString =
    ByteString(codec.encode(t).require.toByteBuffer)

}

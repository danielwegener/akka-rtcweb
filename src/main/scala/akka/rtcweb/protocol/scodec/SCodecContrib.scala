package akka.rtcweb.protocol.scodec

import scodec.{ Err, Codec }
import scodec.bits.BitVector

import scala.math.Ordering
import scalaz.{ \/, -\/, \/- }

/**
 * Additional Combinators for SCodec
 */
object SCodecContrib {

  final def nonZero[A](codec: Codec[A])(implicit ev: Numeric[A]): Codec[A] = codec.validate { case 0 => Err("The value 0 MUST NOT be used") }

  implicit class AwesomeCodecOps[A](codec: Codec[A]) {

    /**
     * Adds a validation to this coded that fails when the partial function applies.
     * @param error a function that can create an error message for a given A.
     * @return
     */
    final def validate(error: PartialFunction[A, Err]): Codec[A] = {
      def unwrap(a: A): Err \/ A = error.lift(a) match {
        case Some(msg) => \/.left[Err, A](msg)
        case None => \/.right(a)
      }
      codec.exmap(unwrap, unwrap)
    }

    def lt[B >: A](b: B)(implicit ordering: Ordering[B]): Codec[A] = codec.exmap(
      {
        case v if ordering.lt(v, b) => \/.right(v)
        case v => \/.right(v)
      }, ???
    )

  }

  /**
   * Codec that supports bit of the form `value ++ (value.size%padding)*0`.
   * @param width width of the padding bytes (the maximum number of appended zeros + 1)
   * @group combinators
   */
  def alignBytes[A](value: Codec[A], width: Int): Codec[A] = alignBits(value, width * 8)

  /**
   * Codec that supports bit of the form `value ++ (value.size%padding)*0`.
   *
   * For example, encoding the string `"hello"` with `fillPadding(ascii, 8)` yields a vector of 8 bytes -- the first 5 byte being
   * being the US-ASCII encoding of `"hello"` and the rest filled with zeros.
   *
   * The `width` denotes the number of bits that should be used for padding.
   *
   * During encoding, the missing padding bytes after an successful decode of the inner value will be consumed and ignored.
   *
   * @param value codec the encodes/decodes the value
   * @param width width of the padding (the maximum number of appended zeros + 1)
   * @group combinators
   */
  def alignBits[A](value: Codec[A], width: Int): Codec[A] =
    new WithPaddingCodec(value, width)

}

private[rtcweb] final class WithPaddingCodec[A](valueCodec: Codec[A], paddingModulo: Int) extends Codec[A] {

  def paddingGap(length: Long, alignSteps: Int): Long = (alignSteps - length % alignSteps) % alignSteps

  override def encode(a: A) = for {
    encA <- valueCodec.encode(a)
    gap = paddingGap(encA.length, paddingModulo)
  } yield encA ++ BitVector.low(gap)

  override def decode(buffer: BitVector) =
    valueCodec.decode(buffer) match {
      case e @ -\/(_) => e
      case \/-((rest, res)) => {
        val gap = paddingGap(buffer.size - rest.size, paddingModulo)
        \/-((rest.drop(gap), res))
      }
    }

  override def toString = s"alignBits($valueCodec, $paddingModulo)"
}

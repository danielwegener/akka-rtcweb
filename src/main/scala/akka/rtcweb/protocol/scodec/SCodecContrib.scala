package akka.rtcweb.protocol.scodec

import scodec.{ Encoder, Err, Codec }
import scodec.bits.BitVector
import scodec.bits.BitVector._
import scodec.codecs._
import shapeless._

import scala.concurrent.duration.{ FiniteDuration, TimeUnit }
import scala.math.Ordering
import scalaz.{ \/, -\/, \/- }

/**
 * Additional Combinators for SCodec
 */
object SCodecContrib {

  final def nonZero[A](codec: Codec[A])(implicit ev: Numeric[A]): Codec[A] = codec.validate { case 0 => Err("The value 0 MUST NOT be used") }

  /**
   * Codec that encodes the length of a given FiniteDuration with a given numeric codec and decodes
   * @param discriminatorCodec a codec for the underlying numeric type
   * @param unit a [[TimeUnit]]
   * @group combinators
   */
  final def duration[A: Numeric](discriminatorCodec: Codec[A], unit: TimeUnit) = discriminatorCodec.xmap[FiniteDuration](
    a => FiniteDuration.apply(implicitly[Numeric[A]].toLong(a), unit),
    a => implicitly[Numeric[A]].fromInt(a.length.toInt))

  /**
   * Codec that always encodes the specified value using an implicit available encoder and always decodes the specified value, returning `()` if the actual bits match
   * the specified bits and returning an error otherwise.
   * @group combinators
   */
  final def constantValue[A](constantValue: A)(implicit encoder: Encoder[A]): Codec[Unit] = scodec.codecs.constant(encoder.encodeValid(constantValue))

  final def variableSizeBytes2[A <: { def length(): Int }, B <: { def length(): Int }](length1: Codec[Int], length2: Codec[Int], value1: Codec[A], value2: Codec[B]): Codec[A :: B :: HNil] = {
    val x: Codec[Int :: Int :: A :: B :: HNil] =
      length1.flatPrepend { len1 =>
        length2 flatPrepend { len2 =>
          fixedSizeBytes(len1, value1) :: fixedSizeBytes(len2, value2)
        }
      }

    val y = x.xmap[A :: B :: HNil](
      { case l1 :: l2 :: v1 :: v2 :: HNil => v1 :: v2 :: HNil },
      { case v1 :: v2 :: HNil => v1.length :: v2.length :: v1 :: v2 :: HNil }
    )
    y
  }

  implicit class AwesomeCodecOps[A](codec: Codec[A]) {

    /**
     * A string terminated by a `null` byte
     * todo: really terminate!
     */
    final def cstring(implicit ev: A <:< String): Codec[A] = codec <~ scodec.codecs.constant(lowByte)

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
   * @param blockWidth width of the padding bytes (the maximum number of appended zero-bytes + 1)
   * @group combinators
   */
  def blockalignBytes[A](value: Codec[A], blockWidth: Int): Codec[A] = blockalignBits(value, blockWidth * 8)

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
   * @param blockWidth width of the padding (the maximum number of appended zeros + 1)
   * @group combinators
   */
  def blockalignBits[A](value: Codec[A], blockWidth: Int): Codec[A] =
    new WithPaddingCodec(value, blockWidth)

}

private[rtcweb] final class WithPaddingCodec[A](valueCodec: Codec[A], paddingModulo: Int) extends Codec[A] {

  def paddingGap(length: Long, alignSteps: Int): Long = (alignSteps - length % alignSteps) % alignSteps

  override def encode(a: A) = for {
    encA <- valueCodec.encode(a)
    gap = paddingGap(encA.length, paddingModulo)
  } yield encA ++ low(gap)

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

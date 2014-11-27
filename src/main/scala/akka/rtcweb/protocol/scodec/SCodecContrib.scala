package akka.rtcweb.protocol.scodec

import java.nio.CharBuffer
import java.nio.charset.{ UnmappableCharacterException, MalformedInputException, Charset }

import scodec.{ Encoder, Err, Codec }
import scodec.bits.BitVector
import scodec.bits.BitVector._
import scodec.codecs._
import shapeless._
import shapeless.ops.hlist._

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
    a => implicitly[Numeric[A]].fromInt(a.toUnit(unit).toInt))

  /**
   * Codec that always encodes the specified value using an implicit available encoder and always decodes the specified value, returning `()` if the actual bits match
   * the specified bits and returning an error otherwise.
   * @group combinators
   */
  final def constantValue[A](constantValue: A)(implicit encoder: Encoder[A]): Codec[Unit] = scodec.codecs.constant(encoder.encodeValid(constantValue))

  object zipper extends Poly1 {
    implicit def sizeAndValueCodec[Si <: Int, Vi] = at[(Si, Codec[Vi])] { case (s, cv) => scodec.codecs.fixedSizeBytes(s, cv) }
  }

  object valueEncoder extends Poly1 {
    implicit def lengthAndValueCodec[Vi] = at[(Vi, Codec[Vi])] { case (v, vc) => vc.encode(v) }
  }

  def multiVariableSizes[SC <: HList, S <: HList, VC <: HList, V <: HList, ZippedLandVCs <: HList, ZippedVandVCs <: HList, SizeLimitedValueCodecs <: HList, EncodedValues <: HList, EncodedValuesUnified <: HList](sizeCodecs: SC, valueCodecs: VC)(implicit scToHListCodec: ToHListCodec.Aux[SC, S],
    vcToHListCodec: ToHListCodec.Aux[VC, V],
    zipSandVCs: Zip.Aux[S :: VC :: HNil, ZippedLandVCs],
    sAndVcMapper: Mapper.Aux[zipper.type, ZippedLandVCs, SizeLimitedValueCodecs],
    zipVandVCs: Zip.Aux[V :: VC :: HNil, ZippedVandVCs],
    vAndVcMapper: Mapper.Aux[valueEncoder.type, ZippedVandVCs, EncodedValues],
    slvcToHListCodec: ToHListCodec.Aux[SizeLimitedValueCodecs, V]): Codec[V] = new Codec[V] {

    override def toString = s"multiVariableSizes($sizeCodecs, $valueCodecs)"

    private val sizeCodec: Codec[S] = scToHListCodec(sizeCodecs)

    override def decode(bits: BitVector): Err \/ (BitVector, V) = {
      sizeCodec.decode(bits).flatMap {
        case (rem, sizes) =>
          val sizeLimitedValueCodecs = (sizes zip valueCodecs).map(zipper)
          val sequenced = slvcToHListCodec(sizeLimitedValueCodecs)
          sequenced.decode(rem)
      }
    }

    import shapeless.test._

    override def encode(v: V): Err \/ BitVector = {
      // encode all values, zip with em over length codecs encoded their lengths and concat bits(length)  ++ bits(values)
      val encodedValues: EncodedValues = (v zip valueCodecs).map(valueEncoder)
      //val encodedValuesSimple = encodedValues.toList.collect{case a: \/[Err, BitVector] => a}

      //typed[List[Err \/ BitVector]](encodedValuesSimple)

      Console.println(this)
      Console.println(encodedValues)
      //Console.println(encodedValuesSimple)
      ???
    }

    private def fail(a: Any, msg: String): Err =
      Err(s"[$a] is too long to be encoded: $msg")
  }

  /**
   * A string terminated by a `null` byte
   */
  final def cstring(codec: Codec[String]): Codec[String] = codec.exmap[String](
    {
      case a if !a.isEmpty && a.indexOf('\0') + 1 == a.length => \/-[String](a.dropRight(1))
      case a => -\/(Err(s"[$a] is not terminated by a nul character or contains multiple nuls"))
    },
    f => \/-(f + '\0')
  )

  implicit class AwesomeCodecOps[A](val codec: Codec[A]) extends AnyVal {

    /**
     * A string terminated by a `null` byte
     * todo: really terminate!
     */
    final def cstring(implicit ev: A =:= String, ve: String =:= A): Codec[String] = SCodecContrib.cstring(codec.xmap(ev, ve))

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

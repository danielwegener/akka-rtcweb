package akka.rtcweb.protocol.scodec

import java.net.InetAddress

import scodec.Attempt.{ Failure, Successful }
import scodec._
import scodec.bits.BitVector._
import scodec.bits.{ BitVector, ByteVector }
import scodec.codecs._
import shapeless._
import shapeless.ops.hlist._

import scala.concurrent.duration.{ FiniteDuration, TimeUnit }
import scala.math.Ordering

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
    a => implicitly[Numeric[A]].fromInt(a.toUnit(unit).toInt)).withToString(s"duration($unit)")

  final val ipv4Address: Codec[InetAddress] = {
    bytes(4).xmap[InetAddress](
      bits => InetAddress.getByAddress(bits.toArray),
      f => ByteVector(f.getAddress)
    )
  }.withToString("ipv4Address")

  final val ipv6Address: Codec[InetAddress] = {
    bytes(32).xmap[InetAddress](
      bits => InetAddress.getByAddress(bits.toArray),
      f => ByteVector(f.getAddress)
    )
  }.withToString("ipv6Address")

  final def xor[A](codec: Codec[A], or: BitVector): Codec[A] = {
    new Codec[A] {
      override def decode(bits: BitVector): Attempt[DecodeResult[A]] =
        codec.decode(bits.xor(or.take(bits.length).padLeft(bits.length))).
          map { case DecodeResult(decoded, rest) => DecodeResult(decoded, bits.drop(bits.length - rest.length)) }
      override def encode(value: A): Attempt[BitVector] = codec.encode(value).map(_.xor(or))

      override def sizeBound: SizeBound = SizeBound.choice(List(codec.sizeBound, SizeBound.exact(or.size)))
    }.withToString(s"xor($codec ^ $or)")
  }

  /**
   * Codec that always encodes the specified value using an implicit available encoder and always decodes the specified value, returning `()` if the actual bits match
   * the specified bits and returning an error otherwise.
   * @group combinators
   */
  final def constantValue[A](constantValue: A)(implicit encoder: Encoder[A]): Codec[Unit] = scodec.codecs.constant(encoder.encode(constantValue).require)

  def boundedSize[A](size:Long, boundedCodec:Codec[A]):Codec[A] = new BoundedSizeCodec[A](size, boundedCodec)
  def boundedSizeBytes[A](size:Long, boundedCodec:Codec[A]):Codec[A] = new BoundedSizeCodec[A](size*8, boundedCodec)

  def multiVariableSizes[SC <: HList, S <: HList, VC <: HList, V <: HList, ZippedLandVCs <: HList, ZippedVandVCs <: HList, SizeLimitedValueCodecs <: HList, EncodedValues <: HList, EncodedValuesUnified <: HList](sizeCodecs: SC, valueCodecs: VC)(implicit scToHListCodec: ToHListCodec.Aux[SC, S],
    vcToHListCodec: ToHListCodec.Aux[VC, V],
    zipSandVCs: Zip.Aux[S :: VC :: HNil, ZippedLandVCs],
    sAndVcMapper: Mapper.Aux[zipper.type, ZippedLandVCs, SizeLimitedValueCodecs],
    zipVandVCs: Zip.Aux[V :: VC :: HNil, ZippedVandVCs],
    vAndVcMapper: Mapper.Aux[valueEncoder.type, ZippedVandVCs, EncodedValues],
    slvcToHListCodec: ToHListCodec.Aux[SizeLimitedValueCodecs, V]): Codec[V] = new Codec[V] {

    override def toString = s"multiVariableSizes($sizeCodecs, $valueCodecs)"

    private val sizeCodec: Codec[S] = scToHListCodec(sizeCodecs)
    private val valueCodec: Codec[V] = vcToHListCodec(valueCodecs)

    override def decode(bits: BitVector): Attempt[DecodeResult[V]] = {
      sizeCodec.decode(bits).flatMap {
        case DecodeResult(sizes, rem) =>
          val sizeLimitedValueCodecs = (sizes zip valueCodecs).map(zipper)
          val sequenced = slvcToHListCodec(sizeLimitedValueCodecs)
          sequenced.decode(rem)
      }
    }

    override def encode(v: V): Attempt[BitVector] = {
      // encode all values, zip with em over length codecs encoded their lengths and concat bits(length)  ++ bits(values)
      val encodedValues: EncodedValues = (v zip valueCodecs).map(valueEncoder)
      //val encodedValuesSimple = encodedValues.toList.collect{case a: \/[Err, BitVector] => a}
      //val encodedValues:ZippedVandVCs = (v zip valueCodecs)//.map(valueEncoder)
      //val encodedValueLengths = encodedValues.map{
      //  case \/-(b:BitVector) => b.length
      //  case e@ -\/ => e
      //}
      valueCodec.encode(v)

      //typed[List[Err \/ BitVector]](encodedValuesSimple)

      ???

    }

    private def fail(a: Any, msg: String): Err =
      Err(s"[$a] is too long to be encoded: $msg")

    override def sizeBound: SizeBound = ???
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

  implicit class AwesomeCodecOps[A](val codec: Codec[A]) extends AnyVal {

    /**
     * Adds a validation to this coded that fails when the partial function applies.
     * @param error a function that can create an error message for a given A.
     * @return
     */
    final def validate(error: PartialFunction[A, Err]): Codec[A] = {
      def unwrap(a: A): Attempt[A] = error.lift(a) match {
        case Some(msg) => Failure(msg)
        case None => Successful(a)
      }
      codec.exmap(unwrap, unwrap)
    }

    def lt[B >: A](b: B)(implicit ordering: Ordering[B]): Codec[A] = codec.exmap(
      {
        case v if ordering.lt(v, b) => Successful(v)
        case v => Successful(v)
      }, ???
    )

  }

  object zipper extends Poly1 {
    implicit def sizeAndValueCodec[Si <: Int, Vi] = at[(Si, Codec[Vi])] { case (s, cv) => scodec.codecs.fixedSizeBytes(s, cv) }
  }

  object valueEncoder extends Poly1 {
    implicit def lengthAndValueCodec[Vi] = at[(Vi, Codec[Vi])] { case (v, vc) => vc.encode(v) }
  }

}

private[rtcweb] final class WithPaddingCodec[A](valueCodec: Codec[A], paddingModulo: Int) extends Codec[A] {

  override def encode(a: A) = for {
    encA <- valueCodec.encode(a)
    gap = paddingGap(encA.length, paddingModulo)
  } yield encA ++ low(gap)

  override def decode(buffer: BitVector) =
    valueCodec.decode(buffer) match {
      case e @ Failure(_) => e
      case Successful(DecodeResult(res, rest)) => {
        val gap = paddingGap(buffer.size - rest.size, paddingModulo)
        Successful(DecodeResult(res, rest.drop(gap)))
      }
    }

  def paddingGap(length: Long, alignSteps: Int): Long = (alignSteps - length % alignSteps) % alignSteps

  override def toString = s"alignBits($valueCodec, $paddingModulo)"

  override def sizeBound: SizeBound = SizeBound.choice(Vector(valueCodec.sizeBound, SizeBound.exact(paddingModulo)))
}

private[scodec] final class BoundedSizeCodec[A](size: Long, codec: Codec[A]) extends Codec[A] {
  require(codec.sizeBound.upperBound.forall(_ <= size), "cannot sizeBound a codec that has a greater upper size-bound than this codec")

  override def sizeBound = codec.sizeBound `|` SizeBound.atMost(size)

  override def encode(a: A) = for {
    encoded <- codec.encode(a)
    result <- {
      if (encoded.size > size)
        Attempt.failure(Err(s"[$a] requires ${encoded.size} bits but field is bounded in size of $size bits"))
      else
        Attempt.successful(encoded)
    }
  } yield result

  override def decode(buffer: BitVector) = {
    codec.decode(buffer.take(size)).map(dr => DecodeResult(dr.value, dr.remainder ++ buffer.drop(size)))

  }

  override def toString = s"boundedSizeBits($size, $codec)"
}


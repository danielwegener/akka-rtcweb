package akka.rtcweb.protocol.scodec

import scodec.{Err, DecodingContext, Codec}
import scodec.bits.BitVector
import shapeless.ops.hlist.{ZipWith, Split, Length, Prepend}
import shapeless._

import scalaz.\/



/**
 * @author Daniel Wegener (Holisticon AG)
 */
object HListCodecContrib {


  def prepend[A, L <: HList](a: Codec[A], l: Codec[L]): Codec[A :: L] = new Codec[A :: L] {
    override def encode(xs: A :: L) = Codec.encodeBoth(a, l)(xs.head, xs.tail)
    override def decode(buffer: BitVector) = Codec.decodeBothCombine(a, l)(buffer) { _ :: _ }
    override def toString = s"$a :: $l"
  }

  object PrependCodec extends Poly2 {
    implicit def caseCodecAndCodecHList[A, L <: HList] = at[Codec[A], Codec[L]](prepend)
  }

  def hzip[L <: HList, V <: HList, LzipV <: HList, POLY, Len <: Nat](sizeCodecs:L, valueCodecs: V)(implicit
                                                                                            lengthL: Length.Aux[L, Len],
                                                                                            lengthV: Length.Aux[V, Len],
                                                                                            zipLV: ZipWith.Aux[L, V, PrependCodec.type, LzipV]
    ): Codec[V] = new Codec[V] {


    /*    override def encode(xs: V) = {
      val (k, l) = xs.split[KLen]
      Codec.encodeBoth(codecK, f(k))(k, l)
    }
    override def decode(buffer: BitVector) = (for {
      k <- DecodingContext(codecK.decode)
      l <- DecodingContext(f(k).decode)
    } yield k ::: l).run(buffer)
    override def toString = s"flatConcat($codecK, $f)"
  */
    override def encode(value: V): \/[Err, BitVector] = {
      sizeCodecs.zipWith(valueCodecs)
      ???
    }

    override def decode(bits: BitVector): \/[Err, (BitVector, V)] = ???
  }

}

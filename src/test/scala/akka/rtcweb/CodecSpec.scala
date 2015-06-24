package akka.rtcweb

import org.specs2.SpecificationLike
import org.specs2.execute.Result
import scodec.bits.BitVector
import scodec.{ DecodeResult, Attempt, Codec }
import shapeless.Lazy

trait CodecSpec extends SpecificationLike {

  protected def roundtrip[A](a: A)(implicit c: Lazy[Codec[A]]): Result = {
    roundtrip(c.value, a)
  }

  protected def roundtrip[A](codec: Codec[A], value: A): Result = {
    val encoded = codec.encode(value)
    encoded.isSuccessful should beTrue
    val Attempt.Successful(DecodeResult(decoded, remainder)) = codec.decode(encoded.require)
    (remainder shouldEqual BitVector.empty) and
      (decoded shouldEqual value)
  }

}

package akka.rtcweb

import org.specs2.SpecificationLike
import org.specs2.execute.{ Success, Result, Failure => TestFailure }
import scodec.Attempt.{ Failure, Successful }
import scodec.bits.BitVector
import scodec.{ DecodeResult, Attempt, Codec }
import shapeless.Lazy

trait CodecSpec extends SpecificationLike {

  protected def roundtrip[A](a: A)(implicit c: Lazy[Codec[A]]): Result = {
    roundtrip(c.value, a)
  }

  protected def roundtrip[A](codec: Codec[A], value: A): Result = {
    codec.encode(value) match {
      case Successful(a) => Success(a.toString())
      case Failure(e) => TestFailure(e.toString())
    }
    val Attempt.Successful(DecodeResult(decoded, remainder)) = codec.decode(codec.encode(value).require)
    (remainder shouldEqual BitVector.empty) and
      (decoded shouldEqual value)
  }

}

package akka.rtcweb

import scodec.Codec
import scodec.Err
import scodec.bits.BitVector

import scalaz.\/

package object protocol {

  def empty[A](default: A): Codec[A] = new Codec[A]() {
    override def encode(value: A): \/[Err, BitVector] = \/.right(BitVector.empty)
    override def decode(bits: BitVector): \/[Err, (BitVector, A)] = \/.right((bits, default))
  }

}


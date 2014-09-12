package akka.rtcweb

import scodec.Codec
import scodec.bits.BitVector

import scalaz.\/

package object protocol {

  def empty[A](default: A): Codec[A] = new Codec[A]() {
    override def encode(value: A): \/[String, BitVector] = \/.right(BitVector.empty)
    override def decode(bits: BitVector): \/[String, (BitVector, A)] = \/.right((bits, default))
  }

}

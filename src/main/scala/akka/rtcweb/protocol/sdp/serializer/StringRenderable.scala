package akka.rtcweb.protocol.sdp.serializer

/**
 * @author Daniel Wegener (Holisticon AG)
 */
trait StringRenderable[T] {
  def toString(t: T): String
}


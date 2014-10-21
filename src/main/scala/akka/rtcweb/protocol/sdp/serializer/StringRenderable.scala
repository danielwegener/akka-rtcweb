package akka.rtcweb.protocol.sdp.serializer

/**
 * @author Daniel Wegener (Holisticon AG)
 */
trait StringRenderable[T] {
  def append(context: StringRenderingContext, t: T): Unit
}

sealed case class SimpleStringRenderable[T](f: T => String) extends StringRenderable[T] {
  @inline override def append(context: StringRenderingContext, t: T): Unit = context.append(f.apply(t))
}

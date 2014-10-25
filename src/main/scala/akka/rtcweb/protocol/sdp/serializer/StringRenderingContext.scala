package akka.rtcweb.protocol.sdp.serializer

/**
 * @author Daniel Wegener (Holisticon AG)
 */
trait RenderingContext[T] {

  def append(str: String): Unit

  def result(): T

}

class StringRenderingContext extends RenderingContext[String] {

  private val stringBuilder: StringBuilder = new StringBuilder

  override def append(str: String): Unit = stringBuilder.append(str)

  override def result(): String = stringBuilder.toString()
}
package akka.rtcweb.protocol.sdp.renderer

import akka.http.util.{Rendering, Renderable, Renderer}
import akka.parboiled2.CharPredicate
import akka.util.ByteString
import scala.annotation.tailrec
import scala.collection.LinearSeq


/**
 * INTERNAL API
 *
 * An entity that can render itself
 */
private[renderer] trait Renderable {
  def render[R <: Rendering](r: R): r.type
}

/**
 * INTERNAL API
 *
 * An entity that can render itself and implements toString in terms of its rendering
 */
private[renderer] trait ToStringRenderable extends Renderable {
  override def toString = render(new StringRendering).get
}

/**
 * INTERNAL API
 *
 * An entity that has a rendered value (like an HttpHeader)
 */
private[renderer] trait ValueRenderable extends ToStringRenderable {
  def value: String = toString
}

private[renderer] trait SingletonValueRenderable extends Product with Renderable {
  private[this] val valueBytes = value.getBytes
  def value = productPrefix
  def render[R <: Rendering](r: R): r.type = r ~ valueBytes
}


trait Renderer[-T] {
  def render[R <: Rendering](r: R, value: T): r.type
}

object Renderer {
  implicit object CharRenderer extends Renderer[Char] {
    def render[R <: Rendering](r: R, value: Char): r.type = r ~ value
  }
  implicit object StringRenderer extends Renderer[String] {
    def render[R <: Rendering](r: R, value: String): r.type = r ~ value
  }
  implicit object CharsRenderer extends Renderer[Array[Char]] {
    def render[R <: Rendering](r: R, value: Array[Char]): r.type = r ~ value
  }

  def caseObjectNameRenderer[T<:Product] : Renderer[T] = new Renderer[T] {
    override def render[R <: Rendering](r: R, value: T) : r.type = r ~ value.productPrefix
  }


  def stringRenderer[T](f:T=>String):Renderer[T] = new Renderer[T] {
    override def render[R <: Rendering](r: R, value: T): r.type = r ~ f(value)
  }

  object RenderableRenderer extends Renderer[Renderable] {
    def render[R <: Rendering](r: R, value: Renderable): r.type = value.render(r)
  }
  implicit def renderableRenderer[T <: Renderable]: Renderer[T] = RenderableRenderer


  def optionRenderer[D, T](defaultValue: D)(implicit sRenderer: Renderer[D], tRenderer: Renderer[T]) =
    new Renderer[Option[T]] {
      def render[R <: Rendering](r: R, value: Option[T]): r.type =
        if (value.isEmpty) sRenderer.render(r, defaultValue) else tRenderer.render(r, value.get)
    }

  def seqRenderer[T: Renderer](separator: String = ", ", empty: String = "") = genericSeqRenderer[String, T](separator, empty)
  def genericSeqRenderer[S, T](separator: S, empty: S)(implicit sRenderer: Renderer[S], tRenderer: Renderer[T]) =
    new Renderer[Seq[T]] {
      def render[R <: Rendering](r: R, value: Seq[T]): r.type = {
        @tailrec def recI(values: IndexedSeq[T], ix: Int = 0): r.type =
          if (ix < values.size) {
            if (ix > 0) sRenderer.render(r, separator)
            tRenderer.render(r, values(ix))
            recI(values, ix + 1)
          } else r

        @tailrec def recL(remaining: LinearSeq[T]): r.type =
          if (remaining.nonEmpty) {
            if (remaining ne value) sRenderer.render(r, separator)
            tRenderer.render(r, remaining.head)
            recL(remaining.tail)
          } else r

        value match {
          case Nil              ⇒ r ~ empty
          case x: IndexedSeq[T] ⇒ recI(x)
          case x: LinearSeq[T]  ⇒ recL(x)
          case x                ⇒ sys.error("Unsupported Seq type: " + x)
        }
      }
    }
}

trait Rendering {
  def ~(char: Char): this.type
  def ~(bytes: Array[Byte]): this.type

  def ~(f: Float): this.type = this ~ f.toString
  def ~(d: Double): this.type = this ~ d.toString

  def ~(int: Int): this.type = this ~ int.toLong

  def ~(long: Long): this.type =
    if (long != 0) {
      val value = if (long < 0) { this ~ '-'; -long } else long
      @tailrec def magnitude(m: Long = 1): Long = if ((value / m) < 10) m else magnitude(m * 10)
      @tailrec def putNextChar(v: Long, m: Long): this.type =
        if (m > 0) {
          this ~ ('0' + (v / m)).toChar
          putNextChar(v % m, m / 10)
        } else this
      putNextChar(value, magnitude())
    } else this ~ '0'


  def ~(string: String): this.type = {
    @tailrec def rec(ix: Int = 0): this.type =
      if (ix < string.length) { this ~ string.charAt(ix); rec(ix + 1) } else this
    rec()
  }



  def ~[T](value: T)(implicit ev: Renderer[T]): this.type = ev.render(this, value)


}

object Rendering {
  val `\"` = CharPredicate('\\', '"')
  case object `, ` extends SingletonValueRenderable // default separator

  case object Empty extends Renderable {
    def render[R <: Rendering](r: R): r.type = r
  }
  case object CRLF extends Renderable {
    def render[R <: Rendering](r: R): r.type = r ~ '\r' ~ '\n'
  }
  case object SP extends Renderable {
    def render[R <: Rendering](r: R): r.type = r ~ ' '
  }

}
class StringRendering extends Rendering {
  private[this] val sb = new java.lang.StringBuilder
  def ~(char: Char): this.type = { sb.append(char); this }
  def ~(bytes: Array[Byte]): this.type = {
    @tailrec def rec(ix: Int = 0): this.type =
      if (ix < bytes.length) { this ~ bytes(ix).asInstanceOf[Char]; rec(ix + 1) } else this
    rec()
  }
  def get: String = sb.toString
}
abstract class ByteArrayBasedRendering(sizeHint: Int) extends Rendering {
  protected var array = new Array[Byte](sizeHint)
  protected var size = 0
  def ~(char: Char): this.type = {
    val oldSize = growBy(1)
    array(oldSize) = char.toByte
    this
  }
  def ~(bytes: Array[Byte]): this.type = {
    if (bytes.length > 0) {
      val oldSize = growBy(bytes.length)
      System.arraycopy(bytes, 0, array, oldSize, bytes.length)
    }
    this
  }
  private def growBy(delta: Int): Int = {
    val oldSize = size
    val neededSize = oldSize.toLong + delta
    if (array.length < neededSize)
      if (neededSize < Int.MaxValue) {
        val newLen = math.min(math.max(array.length.toLong * 2, neededSize), Int.MaxValue).toInt
        val newArray = new Array[Byte](newLen)
        System.arraycopy(array, 0, newArray, 0, array.length)
        array = newArray
      } else sys.error("Cannot create byte array greater than 2GB in size")
    size = neededSize.toInt
    oldSize
  }
}
class ByteArrayRendering(sizeHint: Int) extends ByteArrayBasedRendering(sizeHint) {
  def get: Array[Byte] =
    if (size == array.length) array
    else java.util.Arrays.copyOfRange(array, 0, size)
}
class ByteStringRendering(sizeHint: Int) extends ByteArrayBasedRendering(sizeHint) {
  def get: ByteString = ByteString.fromArray(array, 0, size)
}

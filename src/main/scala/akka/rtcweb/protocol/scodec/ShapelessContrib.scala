package akka.rtcweb.protocol.scodec

/**
 * Allows flattening of HLists
 * Shameless stolen from <a href="https://github.com/milessabin/shapeless/blob/master/examples/src/main/scala/shapeless/examples/flatten.scala">Shapeless Examples</a>
 */
object ShapelessContrib {

  import shapeless._
  import ops.tuple.FlatMapper
  import syntax.std.tuple._
  trait LowPriorityFlatten extends Poly1 {
    implicit def default[T] = at[T](Tuple1(_))
  }
  object flatten extends LowPriorityFlatten {
    implicit def caseTuple[P <: Product](implicit fm: FlatMapper[P, flatten.type]) =
      at[P](_.flatMap(flatten))
  }
}

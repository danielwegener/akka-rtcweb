package akka.rtcweb.protocol.sdp.grouping


sealed trait Semantics
trait SemanticsExtension extends Semantics

object Semantics {
    case object LS extends Semantics
    case object FID extends Semantics
    final case class UnknownSemanticsExtension(name:String) extends SemanticsExtension
  }




final case class Group(semantics: Semantics,streams:Seq[MediaStreamIdentification]) {

}

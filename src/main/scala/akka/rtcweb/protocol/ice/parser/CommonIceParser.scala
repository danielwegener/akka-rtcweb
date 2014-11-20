package akka.rtcweb.protocol.ice.parser

import akka.parboiled2._
import akka.rtcweb.protocol.sdp.parser.CharacterClasses._

private[ice] trait CommonIceParser {
  this: Parser =>

  val `ice-char` = CharPredicate(ALPHA, DIGIT, '+', '/')
  require(`ice-char`.isMaskBased, "BCHAR must be mask based")


  /** {{{component-id = 1*5DIGIT}}} */
  def `component-id`: Rule1[Int] = rule { capture((1 to 5).times(DIGIT) ~ &(!DIGIT)) ~> ((s: String) => s.toInt) }

}

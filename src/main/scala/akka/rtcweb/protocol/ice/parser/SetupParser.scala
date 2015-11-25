package akka.rtcweb.protocol.ice.parser

import akka.parboiled2.{ Rule1, Parser }
import akka.rtcweb.protocol.ice.Setup
import akka.rtcweb.protocol.sdp.parser.{ CharacterClasses, CommonSdpParser, CommonRules }

trait SetupParser {

  this: Parser with CommonRules with CommonSdpParser ⇒

  /** {{{  setup-attr           =  "a=setup:" role}}} **/
  def `setup-attr`: Rule1[Setup] = rule { atomic("a=setup:") ~ role ~> (r => Setup(r)) }

  /**
   * {{{role                 =  "active" / "passive" / "actpass"
   * / "holdconn"}}}
   */
  def `role`: Rule1[Setup.Role] = rule {
    atomic("active") ~ push(Setup.Role.active) |
      atomic("passive") ~ push(Setup.Role.passive) |
      atomic("actpass") ~ push(Setup.Role.actpass) |
      atomic("holdconn") ~ push(Setup.Role.holdconn)
  }

}

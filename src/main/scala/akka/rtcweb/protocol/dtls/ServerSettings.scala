package akka.rtcweb.protocol.dtls

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps

object ServerSettings {
  def apply(settings: ServerSettings): ServerSettings = ServerSettings(Timeouts(5 seconds))

  final case class Timeouts(bindTimeout: FiniteDuration)

}

final case class ServerSettings(
    timeouts: ServerSettings.Timeouts) {

}

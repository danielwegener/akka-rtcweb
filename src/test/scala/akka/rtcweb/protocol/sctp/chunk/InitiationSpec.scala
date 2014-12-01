package akka.rtcweb.protocol.sctp.chunk

import akka.rtcweb.protocol.sctp.chunk.Initiation._
import org.scalatest.{ Matchers, Inside, WordSpec }
import scodec.bits._
import scala.concurrent.duration._
import scalaz.{\/-, -\/}

class InitiationSpec extends WordSpec with Matchers with Inside {

  "An Initiation" should {
    "be pickable back and forth" in {
      val in = Initiation(1, 1, 1, 1, 1, Vector[InitiationParameter](
        `IPv4 Address`(hex"00000001"),
        `IPv6 Address`(hex"00000000000000000000000000000001"),
        `Cookie Preservative`(100 seconds),
        `Host Name Address`("host.name"),
        `Padding Parameter`(255),
        `Supported Address Types`(Vector(`Address Type`.IPv4, `Address Type`.`Host Name`)),
        `Forward-TSN-Supported`()
      ))

      val decoded = Initiation.codec.encode(in).flatMap(Initiation.codec.decode)
      inside(decoded) {
        case \/-((BitVector.empty, out)) => in should be(out)
        case -\/(e) => fail(e.message)
      }

    }
  }

}

package akka.rtcweb.protocol.ice

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.ActorSystem
import akka.io.{IO, Udp}
import akka.pattern.ask
import akka.testkit._
import akka.util.Timeout
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class IceAgentSpec extends TestKitBase
    with WordSpecLike with Matchers with BeforeAndAfterAll
    with Inspectors with OptionValues with ImplicitSender {
  10.milliseconds.dilated

  implicit lazy val system = ActorSystem()
  implicit val timeout = Timeout(10 seconds)
  implicit val context = system.dispatcher

  val udpSender = new InetSocketAddress(InetAddress.getLoopbackAddress, 1337)

  override def afterAll() {
    shutdown()
  }



  "IceAgent" should {

    "go on a discovery" in {
      val unitRef = TestActorRef[IceAgent](IceAgent.props(Vector()))

      val block = for {
        boundEvent <- (IO(Udp) ? Udp.Bind(unitRef, new InetSocketAddress("::", 0))).mapTo[Udp.Bound]
        two <- Future(unitRef ! boundEvent)
        expect <- Future(expectNoMsg(5 seconds))
      } yield expect

      Await.result(block, 10 seconds)
    }
  }

}

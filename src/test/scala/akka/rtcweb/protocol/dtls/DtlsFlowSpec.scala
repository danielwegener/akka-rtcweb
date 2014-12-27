package akka.rtcweb.protocol.dtls

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import akka.actor.ActorSystem
import org.scalatest.{ WordSpecLike, MustMatchers }

import scala.concurrent.duration._
import akka.io.IO
import akka.stream.io.StreamUdp
import akka.stream.scaladsl.Flow
import akka.stream.{ StreamSubscriptionTimeoutTerminationMode, StreamSubscriptionTimeoutSettings, FlowMaterializer, MaterializerSettings }
import akka.testkit.TestProbe
import akka.util.ByteString

import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

class DtlsFlowSpec extends WordSpecLike with MustMatchers { // extends AkkaSpec

  val settings = MaterializerSettings(
    initialInputBufferSize = 4,
    maxInputBufferSize = 4,
    dispatcher = "akka.test.stream-dispatcher",
    subscriptionTimeoutSettings = StreamSubscriptionTimeoutSettings(StreamSubscriptionTimeoutTerminationMode.WarnTermination, 10 seconds),
    fileIODispatcher = "akka.test.fileio-dispatcher")

  implicit val actorSystem = ActorSystem("dtls-flow-spec")
  implicit val materializer = FlowMaterializer(None)

  def temporaryServerAddress: InetSocketAddress = {
    val serverSocket = DatagramChannel.open().socket()
    serverSocket.bind(new InetSocketAddress("127.0.0.1", 0))
    val address = new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort)
    serverSocket.close()
    address
  }

  def connect: StreamDtls.DtlsConnection = {
    val connectProbe = TestProbe()
    connectProbe.send(IO(StreamDtls), StreamDtls.Bind(settings, new InetSocketAddress("127.0.0.1", 0)))
    connectProbe.expectMsgType[StreamDtls.DtlsConnection]
  }

  def echoServer(serverAddress: InetSocketAddress = temporaryServerAddress) = {
    val server = bind(serverAddress)
    server.inputStream.subscribe(server.outputStream)
  }

  def bind(serverAddress: InetSocketAddress = temporaryServerAddress): StreamUdp.UdpConnection = {
    val bindProbe = TestProbe()
    bindProbe.send(IO(StreamDtls), StreamDtls.Bind(settings, serverAddress))
    bindProbe.expectMsgType[StreamUdp.UdpConnection]
  }

  "DTLS listen stream" must {
    "be able to implement echo" in {
      val serverAddress = temporaryServerAddress
      val server = echoServer(serverAddress)
      val conn = connect

      val testInput = Iterator.fill(20)(ByteString('A'), serverAddress)
      val expectedOutput = ByteString(Array.fill[Byte](10)('A'))

      // send 20 but just read 10 as UDP is unreliable
      //todo: does not compile
      //Flow(testInput).toPublisher(materializer).subscribe(conn.outputStream)

      //conn.inputStream.
      //val resultFuture: Future[ByteString] = Flow(conn.inputStream).take(10).
      //  fold(ByteString.empty)((acc, in) ⇒ acc ++ in).toFuture()

      //Await.result(resultFuture, 3.seconds) must be(expectedOutput)
    }
  }

}

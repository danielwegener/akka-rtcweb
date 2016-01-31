package akka.rtcweb

import java.net.InetSocketAddress

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.http.scaladsl.util.FastFuture
import akka.parboiled2.ParserInput
import akka.rtcweb.protocol.ice.IceAgent.AgentRole
import akka.rtcweb.protocol.jsep.RTCPeerConnection
import akka.rtcweb.protocol.jsep.RTCPeerConnection._
import akka.rtcweb.protocol.{ RtcWebSDPParser, RtcWebSDPRenderer }
import akka.rtcweb.protocol.ice.IceAgent
import akka.rtcweb.protocol.sdp.SessionDescription
import akka.stream.ActorMaterializer
import akka.stream.io.InterfaceMonitor
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.{ Source, StdIn }
import scala.language.postfixOps

object RtcwebServer {

  import Directives._
  implicit val system = ActorSystem("RtcwebServer")
  implicit val askTimeout: Timeout = 500.millis
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()(system)

  val sdpMediaType = MediaType.customWithFixedCharset("application", "sdp", HttpCharsets.`UTF-8`)
  //(IO(StreamDtls) ? StreamDtls.Bind(materializer.settings, InetSocketAddress.createUnresolved("127.0.0.1", 4242))).mapTo[StreamDtls.DtlsConnection]
  val f = Source.fromInputStream(getClass.getResourceAsStream("/index.html")).getLines().mkString("\n")
  val interfaceMonitor = system.actorOf(InterfaceMonitor.props(1 seconds))
  val index = HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, f))

  implicit val sdum = PredefinedFromEntityUnmarshallers.byteArrayUnmarshaller.flatMap(_ => { _ => { bs => FastFuture(RtcWebSDPParser.parserFor(ParserInput(bs)).sdp.run()) } })

  val renderer = new RtcWebSDPRenderer
  implicit val sessionDescriptionToStringMarshaller =
    PredefinedToEntityMarshallers.stringMarshaller(sdpMediaType).wrap(sdpMediaType)(renderer.render)

  val rtcWebExtension = RtcWeb.get(system)
  val iceServers = Vector(StunServerDescription(InetSocketAddress.createUnresolved("stun1.l.google.com", 19302)))
  val iceAgent = system.actorOf(IceAgent.props(AgentRole.Controlled, system.deadLetters, iceServers, 1024 to 2048), "Ice-Agent")
  val peerConnection = system.actorOf(RTCPeerConnection.props(PeerConnectionConfiguration(Vector.empty, 10, BundlePolicy.`max-compat`), iceAgent), "PeerConnection")

  val api = {
    (get | post) {
      path("") {
        complete(index)
      } ~
        path("offer") {

          get {
            extractLog { log =>
              val response = (peerConnection ? RTCPeerConnection.CreateOffer(RTCOfferOptions())).mapTo[RTCSessionDescription.offer].map(_.sessionDescription)

              log.info("returning: " + response)
              complete(response)
            }
          } ~ post {
            extractLog { log =>
              entity(as[SessionDescription]) { sd =>
                peerConnection ? RTCPeerConnection

                log.info("received and parsed: " + sd.toString)
                log.info("returning: " + sd)
                complete(sd)
              }
            }
          }

        } ~
        path("ping") {
          complete("PONG!")
        } ~
        path("crash") {
          complete(sys.error("BOOM!"))
        }
    }
  }

  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(api, "127.0.0.1", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine()
    system.terminate()

  }

}

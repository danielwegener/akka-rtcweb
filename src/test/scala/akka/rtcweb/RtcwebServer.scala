package akka.rtcweb

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.http.scaladsl.util.FastFuture
import akka.rtcweb.protocol.RtcWebSDPRenderer
import akka.rtcweb.protocol.ice.StunServer
import akka.rtcweb.protocol.sdp.SessionDescription
import akka.rtcweb.protocol.sdp.parser.SessionDescriptionParser
import akka.stream.ActorMaterializer
import akka.stream.io.InterfaceMonitor
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.{ Source, StdIn }
import scala.language.postfixOps

object RtcwebServer extends Directives {

  implicit val system = ActorSystem("RtcwebServer")
  implicit val askTimeout: Timeout = 500.millis
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()(system)

  val sdpMediaType = MediaType.custom("application/sdp", MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`))
  //(IO(StreamDtls) ? StreamDtls.Bind(materializer.settings, InetSocketAddress.createUnresolved("127.0.0.1", 4242))).mapTo[StreamDtls.DtlsConnection]
  val f = Source.fromInputStream(getClass.getResourceAsStream("/index.html")).getLines().mkString("\n")
  val interfaceMonitor = system.actorOf(InterfaceMonitor.props(1 seconds))
  val index = HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, f))

  implicit val sdum = PredefinedFromEntityUnmarshallers.stringUnmarshaller.flatMap(_=> {_ => { sd => FastFuture(SessionDescriptionParser.parse(sd))}})
  val stunServer = system.actorOf(StunServer.props())

  val renderer = new RtcWebSDPRenderer
  implicit val sessionDescriptionToStringMarshaller =
    PredefinedToEntityMarshallers.stringMarshaller(sdpMediaType).wrap(sdpMediaType)(renderer.render)

  val api = {
    (get | post) {
      path("") {
        complete(index)
      } ~
        path("offer") {
          extractLog { log =>
            entity(as[SessionDescription]) { sd =>
              log.info("received and parsed: " + sd.toString)
              log.info("returning: " + sd)
              complete(sd)
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

  def main(args: Array[String]) = {
    Http().bindAndHandle(api, "127.0.0.1", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine()
    system.shutdown()

  }

}

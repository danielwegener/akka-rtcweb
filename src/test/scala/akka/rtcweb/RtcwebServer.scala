
import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model._
import akka.http.server.directives.BasicDirectives._
import akka.http.unmarshalling.{ Unmarshal, Unmarshaller }
import akka.io.IO
import akka.rtcweb.protocol.sdp.SessionDescription
import akka.rtcweb.protocol.sdp.parser.SessionDescriptionParser
import akka.rtcweb.protocol.sdp.renderer.SdpRendering
import akka.stream.FlowMaterializer
import akka.util.Timeout
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.pattern.ask

import scala.io.Source

object RtcwebServer extends App {

  implicit val system = ActorSystem("RtcwebServer")

  import system.dispatcher

  //implicit val materializer = FlowMaterializer(MaterializerSettings(system))

  implicit val materializer = FlowMaterializer()

  implicit val askTimeout: Timeout = 500.millis

  //(IO(StreamDtls) ? StreamDtls.Bind(materializer.settings, InetSocketAddress.createUnresolved("127.0.0.1", 4242))).mapTo[StreamDtls.DtlsConnection]

  val httpBindingFuture = (IO(Http) ? Http.Bind(interface = "127.0.0.1", port = 8080)).mapTo[Http.ServerBinding]

  val sdpMediaType = MediaType.custom("application/sdp")

  val f = Source.fromInputStream(getClass.getResourceAsStream("/index.html")).getLines().mkString("\n")
  val index = HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, f))

  implicit val toSessionDescriptionUnmarshaller = Unmarshaller((SessionDescriptionParser.parse _).andThen(a=>Future.apply(a)))

  import akka.http.server.ScalaRoutingDSL._

  handleConnections(httpBindingFuture) withRoute {
    (get | post) {
      path("") {
        complete(index)
      } ~
        path("offer") {
          extractLog { log =>
            extract(_.request.entity) { entity =>

              val r = Unmarshal(entity).to[String].flatMap[SessionDescription](s => Future(SessionDescriptionParser.parse(s)))

              val result = Await.result(r, Duration.Inf)
              log.info("received and parsed: " + result.toString)
              val response = SdpRendering.render(result)
              log.info("returning: " + response)
              complete(HttpResponse(entity = response))
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

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Console.readLine()
  system.shutdown()

  ////////////// helpers //////////////

}

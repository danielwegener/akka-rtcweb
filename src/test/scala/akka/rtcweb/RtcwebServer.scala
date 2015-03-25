package akka.rtcweb

import akka.actor.ActorSystem
import akka.http._
import akka.http.model._
import akka.http.unmarshalling.{Unmarshal, Unmarshaller}
import akka.rtcweb.protocol.RtcWebSDPRenderer
import akka.rtcweb.protocol.sdp.SessionDescription
import akka.rtcweb.protocol.sdp.parser.SessionDescriptionParser
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

object RtcwebServer extends App {

  implicit val system = ActorSystem("RtcwebServer")

  import akka.rtcweb.RtcwebServer.system.dispatcher
  implicit val askTimeout: Timeout = 500.millis
  val sdpMediaType = MediaType.custom("application/sdp")

  //(IO(StreamDtls) ? StreamDtls.Bind(materializer.settings, InetSocketAddress.createUnresolved("127.0.0.1", 4242))).mapTo[StreamDtls.DtlsConnection]
  val f = Source.fromInputStream(getClass.getResourceAsStream("/index.html")).getLines().mkString("\n")
  val index = HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, f))
  implicit val toSessionDescriptionUnmarshaller = Unmarshaller((SessionDescriptionParser.parse _).andThen(a => Future.apply(a)))
  val renderer = new RtcWebSDPRenderer
  val api = {
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
              val response = renderer.render(result)
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
  import akka.http.server.Directives._
  val binding = Http().bind(interface = "127.0.0.1", port = 8080).to(Sink.foreach { conn ⇒
    val mat = conn.flow.join(api).run()
  }).run()
  private implicit val materializer = ActorFlowMaterializer()(system)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Console.readLine()
  system.shutdown()

}

package akka.rtcweb

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.{ Unmarshal, Unmarshaller }
import akka.http.scaladsl.util.FastFuture
import akka.rtcweb.protocol.RtcWebSDPRenderer
import akka.rtcweb.protocol.sdp.parser.SessionDescriptionParser
import akka.stream.ActorFlowMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.io.{ StdIn, Source }

object RtcwebServer extends Directives {

  implicit val system = ActorSystem("RtcwebServer")
  private implicit val materializer = ActorFlowMaterializer()(system)

  import akka.rtcweb.RtcwebServer.system.dispatcher
  implicit val askTimeout: Timeout = 500.millis
  val sdpMediaType = MediaType.custom("application/sdp", MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`))

  //(IO(StreamDtls) ? StreamDtls.Bind(materializer.settings, InetSocketAddress.createUnresolved("127.0.0.1", 4242))).mapTo[StreamDtls.DtlsConnection]
  val f = Source.fromInputStream(getClass.getResourceAsStream("/index.html")).getLines().mkString("\n")

  val index = HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, f))
  implicit val toSessionDescriptionUnmarshaller = Unmarshaller(_ => (SessionDescriptionParser.parse _).andThen(FastFuture.apply))

  val renderer = new RtcWebSDPRenderer
  val api = {
    (get | post) {
      path("") {
        complete(index)
      } ~
        path("offer") {
          extractLog { log =>
            extract(_.request.entity) { entity =>

              val sd = Unmarshal(entity).to[String].flatMap(toSessionDescriptionUnmarshaller.apply)

              val result = Await.result(sd, Duration.Inf)
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

  def main(args: Array[String]) = {
    Http().bindAndHandle(api, "127.0.0.1", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine()
    system.shutdown()

  }

}

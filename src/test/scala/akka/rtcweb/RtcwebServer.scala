
import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model._
import akka.io.IO
import akka.stream.scaladsl2.FlowMaterializer
import akka.util.Timeout
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


  import akka.http.server.ScalaRoutingDSL._

  handleConnections(httpBindingFuture) withRoute {
    get {
      path("") {
        complete(index)
      } ~
      /*path("offer") {
        //post {
          complete(HttpResponse(StatusCodes.NoContent))
        //}
      } ~*/
        path("ping") {
          complete("PONG!")
        } ~
        path("crash") {
          complete(sys.error("BOOM!"))
        }
    }
  }

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  val f = Source.fromInputStream(getClass.getResourceAsStream("/index.html")).getLines().mkString("\n")
  val index = HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, f))

  Console.readLine()
  system.shutdown()

  ////////////// helpers //////////////




}

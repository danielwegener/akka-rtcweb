import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model.HttpMethods._
import akka.http.model._
import akka.io.IO
import akka.stream.scaladsl.Flow
import akka.stream.{ MaterializerSettings, FlowMaterializer }
import akka.util.Timeout
import com.typesafe.config.{ ConfigFactory, Config }
import scala.concurrent.duration._
import akka.pattern.ask

object RtcwebServer extends App {
  val testConf: Config = ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.log-dead-letters = off
                                                   """)
  implicit val system = ActorSystem("ServerTest", testConf)
  import system.dispatcher

  val requestHandler: HttpRequest ⇒ HttpResponse = {
    case HttpRequest(GET, Uri.Path("/"), _, _, _) ⇒ index
    case HttpRequest(POST, Uri.Path("/offer"), _, entity, _) ⇒
      system.log.info(s"received offer: $entity"); HttpResponse(200, entity = "Unknown resource!")
    case _: HttpRequest ⇒ HttpResponse(404, entity = "Unknown resource!")
  }

  implicit val materializer = FlowMaterializer(MaterializerSettings(testConf))

  implicit val askTimeout: Timeout = 500.millis
  val bindingFuture = IO(Http) ? Http.Bind(interface = "localhost", port = 8080)
  bindingFuture foreach {
    case Http.ServerBinding(localAddress, connectionStream) ⇒
      Flow(connectionStream).foreach({
        case Http.IncomingConnection(remoteAddress, requestPublisher, responseSubscriber) ⇒
          println("Accepted new connection from " + remoteAddress)
          Flow(requestPublisher).map(requestHandler).produceTo(responseSubscriber)
      })
  }

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Console.readLine()
  system.shutdown()

  ////////////// helpers //////////////

  lazy val index = HttpResponse(
    entity = HttpEntity(MediaTypes.`text/html`,
      """|<html>
        | <head>
        | <script type="text/javascript">
        | var iceServers = {
        |    iceServers: [{
        |        url: 'stun:stun.l.google.com:19302'
        |    }]
        |};
        |
        |var optionalRtpDataChannels = {
        |    optional: [{
        |        RtpDataChannels: true
        |    }]
        |};
        |
        | </script>
        | </head>
        | <body>
        |
        |    <h1>Say hello to <i>akka-http-core</i>!</h1>
        |    <p>Defined resources:</p>
        |    <ul>
        |      <li><a href="/ping">/ping</a></li>
        |      <li><a href="/crash">/crash</a></li>
        |    </ul>
        |  </body>
        |</html>""".stripMargin))
}

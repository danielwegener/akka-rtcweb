package akka.rtcweb.protocol.dtls

import akka.actor.{ Actor, ActorLogging, Props }

class StreamDtlsManager extends Actor with ActorLogging {

  /**
   * stolen from [akka.http.HttpManager#receive].
   * @return
   */
  override def receive: Receive = {
    case StreamDtls.Bind(endpoint, options, settings) ⇒
      log.debug("Binding to {}", endpoint)
      val commander = sender()

    /*val effectiveSettings = ServerSettings(settings)
      val udpBind = StreamUdp.Bind(materializerSettings, endpoint, options)
      val askTimeout = Timeout(effectiveSettings.timeouts.bindTimeout + 5.seconds) // FIXME: how can we improve this?
      val tcpServerBindingFuture = IO(StreamUdp)(context.system).ask(udpBind)(askTimeout)
      tcpServerBindingFuture onComplete {
        case Success(udpConnection @ StreamUdp.UdpConnection(localAddress, inputStream, outputStream)) ⇒
          log.info("Bound to {}", localAddress)
          val materializer = FlowMaterializer(materializerSettings)
          val dtlsServerPipeline = new ServerPipeline(effectiveSettings, materializer, log)
          val dtlsConnection = dtlsServerPipeline.apply(udpConnection)
          commander ! dtlsConnection
        case Failure(error) ⇒
          log.warning("Bind to {} failed due to ", endpoint, error)
          commander ! Status.Failure(error)
      }
      */

  }

}

private[dtls] object StreamDtlsManager {
  def props(dtlsSettings: StreamDtlsExt#Settings) =
    Props(new StreamDtlsManager()) withDispatcher dtlsSettings.ManagerDispatcher
}
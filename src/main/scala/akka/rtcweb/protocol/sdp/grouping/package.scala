package akka.rtcweb.protocol.sdp

/**
 * This package implements the structures for rfc5888: The Session Description Protocol (SDP) Grouping Framework [[https://tools.ietf.org/html/rfc5888]]
 */
package object grouping {

  implicit class SessionDescriptionGroupingOps(private val s: SessionDescription) extends AnyVal {

    def groupedMediaDescriptions: Map[String, MediaDescription] = {
      //s.mediaDescriptions.groupBy( _.mediaAttributes. )
      ???
    }

  }

}


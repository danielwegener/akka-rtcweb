package akka.rtcweb.protocol

import scala.annotation.{ StaticAnnotation }

/**
 * Marks typs that should never cross the wire.
 */
class DoNotExport extends StaticAnnotation

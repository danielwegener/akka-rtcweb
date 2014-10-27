package akka.rtcweb.protocol.sctp.chunk

//case class InitAck()

object InitAck {

  /**
   * The INIT ACK chunk is used to acknowledge the initiation of an SCTP
   * association.
   *
   * The parameter part of INIT ACK is formatted similarly to the INIT
   * chunk.  It uses two extra variable parameters: The State Cookie and
   * the Unrecognized Parameter:
   *
   * The format of the INIT ACK chunk is shown below:
   * {{{
   *
   *
   * 0                   1                   2                   3
   * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |   Type = 2    |  Chunk Flags  |      Chunk Length             |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                         Initiate Tag                          |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |              Advertised Receiver Window Credit                |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |  Number of Outbound Streams   |  Number of Inbound Streams    |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * |                          Initial TSN                          |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * \                                                               \
   * /              Optional/Variable-Length Parameters              /
   * \                                                               \
   * }}}
   */
  implicit val codec = ???

}

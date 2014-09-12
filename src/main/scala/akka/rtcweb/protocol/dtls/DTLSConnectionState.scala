package akka.rtcweb.protocol.dtls

/**
 *
 * A TLS connection state is the operating environment of the TLS Record
 * Protocol.  It specifies a compression algorithm, an encryption
 * algorithm, and a MAC algorithm.  In addition, the parameters for
 * these algorithms are known: the MAC key and the bulk encryption keys
 * for the connection in both the read and the write directions.
 * Logically, there are always four connection states outstanding: the
 * current read and write states, and the pending read and write states.
 * All records are processed under the current read and write states.
 * The security parameters for the pending states can be set by the TLS
 * Handshake Protocol, and the ChangeCipherSpec can selectively make
 * either of the pending states current, in which case the appropriate
 * current state is disposed of and replaced with the pending state; the
 * pending state is then reinitialized to an empty state.  It is illegal
 * to make a state that has not been initialized with security
 * parameters a current state.  The initial current state always
 * specifies that no encryption, compression, or MAC will be used.
 *
 */
case class DTLSConnectionState(
  securityParameters: SecurityParameters //todo continue
  )
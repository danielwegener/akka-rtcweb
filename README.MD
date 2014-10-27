# akka-rtcweb-experimental

This project aims to provide an akka serverside implementation of the rtcweb protocol stack focused on the RTCDataChannels.

RTCWEB allows akka systems to establish (somewhat) encrypted low-latency channels with browser sessions or other actor systems. These channels support
partial ordering and relaxed reliability guarantees (like udp does) and thus can still reach a very low latencies on unreliable or slow connections
 (compared to WebSockets or any AJAX-Polling). Additionally rtcweb supports multihoming and automatic reestablishing of connections which is a nice feature for network-jumping clients.
  Thus this stack, even beeing a skyscraper, could lead to amazing new applications where actor systems participate in browser p2p communications or create decentralized dynamic clusters.

This is just a just for fun private project and aims to implement a minimal working example. Any help, hints and contributions are welcome!

The following protocols and RFCs are (partially) covered (you need to love network protocols):

* [draft-ietf-rtcweb-jsep-07: Javascript Session Establishment Protocol](http://tools.ietf.org/html/draft-ietf-rtcweb-jsep-07) aka [WebRTC 1.0: Real-time Communication Between Browsers](http://www.w3.org/TR/webrtc/)
* [rfc4566: SDP: Session Description Protocol](https://tools.ietf.org/html/rfc4566) (marshalling and negotiation)
* [draft-ietf-rtcweb-data-protocol-08: DCEP - WebRTC Data Channel Establishment Protocol](https://tools.ietf.org/html/draft-ietf-rtcweb-data-protocol-08)
* [rfc4960: SCTP - Stream Control Transmission Protocol](https://tools.ietf.org/html/rfc4960) with [rfc4960: Stream Control Transmission Protocol (SCTP) Partial Reliability Extension](https://tools.ietf.org/html/rfc3758)
    * [Padding Chunk and Parameter for the Stream Control Transmission Protocol (SCTP)](https://tools.ietf.org/html/rfc4820)
* [rfc4347: Datagram Transport Layer Security Version](https://tools.ietf.org/html/rfc4347)
    * [draft-ietf-tsvwg-sctp-dtls-encaps-05: DTLS Encapsulation of SCTP Packets](https://tools.ietf.org/html/draft-ietf-tsvwg-sctp-dtls-encaps-05)

Just yet many many thanks to [scodec](https://github.com/scodec/scodec) and [parboiled2](https://github.com/sirthias/parboiled2) which makes encoding and parsing a breeze!

## Build it!

currently built against the akka:2.3-SNAPSHOT (for early akka-http support that is not in the http-and-streams-experimental releases).
so before you start: checkout akka release-2.3-dev and run `sbt publish-local -Dakka.scaladoc.diagrams=false`
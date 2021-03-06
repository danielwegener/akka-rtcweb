> WORK IN PROGRESS!

# akka-rtcweb-experimental

This project aims to provide an akka server-side implementation of the rtcweb protocol stack focused on the RTCDataChannels ([High Performance Browser Networking, Ch. 8](http://chimera.labs.oreilly.com/books/1230000000545/ch18.html) by [@igrigorik](https://twitter.com/igrigorik/) gives a very good introduction on this topic). This is achieved by implementing [draft-ietf-rtcweb-gateways-00: WebRTC Gateways](https://tools.ietf.org/html/draft-ietf-rtcweb-gateways-00) with the following details:

- ICE-Support for TURN/Trickle (no TURN yet, so no full ICE implementation)
- Support for DataChannels
- Since we are only interested in the Data channels at the moment, we can ignore SRTP (yet, not conforming to the spec)


RTCWEB allows akka systems to establish (somewhat) encrypted low-latency DataChannels with user-agents or other actor systems. These channels support configurable reliability semantics like partial ordering and relaxed reliability guarantees (like udp does) and thus can still reach a very low latencies on unreliable or slow connections
 (compared to WebSockets or SSE). Additionally rtcweb supports multihoming and automatic reestablishing of connections which is a nice feature for moving, multi-homed clients.
  Thus this stack, even being a skyscraper, could lead to amazing new applications where actor systems participate in browser P2P-communication or create decentralized dynamic clusters.

This is just a just for fun private project and aims to implement a minimal working example. Any help, hints and contributions are welcome!
I expect this solution to be _not so fast_ since a lot of low level protocol plumbing is implemented with higher level constructs.
The following protocols and RFCs are (partially) covered (you need to love network protocols):


## The Stack

          <Transport>        <Signaling>
      +-----------------+
      |   WebRTC        |
      |   Data Channels |
      |   (rfc4960)     |
      +-----------------+
      | SCTP (rfc4960)  |
      +-----------------+
      |    DTLS 1.2     |------------------+ 
      |(draft-ietf-rt..)|   JSEP           |
      +-----------------+------------------+
      |   ICE/STUN      |   SDP            |
      +-----------------+------------------+
      |        (UDP)    |   (akka-http)    |
      +------------------------------------+
      |             (akka-io)              |     
      +------------------------------------+

## User Api

* [draft-ietf-rtcweb-jsep-10: Javascript Session Establishment Protocol](http://tools.ietf.org/html/draft-ietf-rtcweb-jsep-10) aka [WebRTC 1.0: Real-time Communication Between Browsers](http://www.w3.org/TR/webrtc/)
* [An Offer/Answer Model with the Session Description Protocol (SDP)](https://tools.ietf.org/html/rfc3264)
    * [rfc4566: SDP: Session Description Protocol](https://tools.ietf.org/html/rfc4566) (marshalling and negotiation)
    * [rfc5888: The Session Description Protocol (SDP) Grouping Framework](https://tools.ietf.org/html/rfc5888)
    * [rfc3556: Session Description Protocol (SDP) Bandwidth Modifiers for RTP Control Protocol (RTCP) Bandwidth](https://tools.ietf.org/html/rfc3556) (specifying )
    * [rfc4572: Connection-Oriented Media Transport over the Transport Layer Security (TLS) Protocol in the Session Description Protocol (SDP)](https://tools.ietf.org/html/rfc4572)
    * [draft-ietf-mmusic-sctp-sdp-14:Stream Control Transmission Protocol (SCTP)-Based Media Transport in the Session Description Protocol (SDP)](https://tools.ietf.org/html/draft-ietf-mmusic-sctp-sdp-14)

## Network Protocols

__Base protocols:__

- [rfc6347: Datagram Transport Layer Security Version 1.2](https://tools.ietf.org/html/rfc6347)
- [rfc5245: Interactive Connectivity Establishment (ICE): A Protocol for Network Address Translator (NAT) Traversal for Offer/Answer Protocols](https://tools.ietf.org/html/rfc5245#section-4.3)
- [rfc5389: Session Traversal Utilities for NAT (STUN)](https://tools.ietf.org/html/rfc5389)

- [rfc4960: SCTP - Stream Control Transmission Protocol](https://tools.ietf.org/html/rfc4960) (Describes how SCTP works)
    - [rfc4960: Stream Control Transmission Protocol (SCTP) Partial Reliability Extension](https://tools.ietf.org/html/rfc3758) 
    - [rfc4820: Padding Chunk and Parameter for the Stream Control Transmission Protocol (SCTP)](https://tools.ietf.org/html/rfc4820) <br> Defines Padding Messages for SCTP to support MTU Discovery
    - [rfc6525: Stream Control Transmission Protocol (SCTP) Stream Reconfiguration](https://tools.ietf.org/html/rfc6525) <br> Defines Reconfiguration of multiplexed application data channels over a single SCTP association

__WebRTC related:__

- [draft-ietf-rtcweb-data-channel-13: WebRTC Data Channels](https://tools.ietf.org/html/draft-ietf-rtcweb-data-channel-13)
    - [draft-ietf-rtcweb-data-protocol-09: DCEP - WebRTC Data Channel Establishment Protocol](https://tools.ietf.org/html/draft-ietf-rtcweb-data-protocol-09) (Describes how DataChannels lifecycle can be controlled on a SCTP connection.)
    - [draft-ietf-tsvwg-sctp-dtls-encaps-09: DTLS Encapsulation of SCTP Packets](https://tools.ietf.org/html/draft-ietf-tsvwg-sctp-dtls-encaps-09) (Describes how SCTP runs on top of DTLS)

## Acknowledgments

Many many thanks to [scodec](https://github.com/scodec/scodec) and [parboiled2](https://github.com/sirthias/parboiled2). They make encoding/decoding and parsing a breeze!

## Build it!

`sbt package` :)
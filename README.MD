## Akka RTCWEB

This project aims to provide an akka serverside implementation of the rtcweb protocol stack focused on the RTCDataChannels.

This will allow akka applications to establish low-latency channels with browser sessions. These channels support
relaxed ordering and reliability guarantees (like udp does) and thus can reach a very low latencies.






currently built against the akka:2.3-SNAPSHOT (for early akka-http support that is not in the http-and-streams-experimental releases).

so at first: checkout akka release-2.3-dev and run `sbt publish-local -Dakka.scaladoc.diagrams=false` 
currently built against the akka:2.3-SNAPSHOT (for early akka-http support that is not in the http-and-streams-experimental releases).

so at first: checkout akka release-2.3-dev and run `sbt publish-local -Dakka.scaladoc.diagrams=false` 
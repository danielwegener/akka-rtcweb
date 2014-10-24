import ScalaJSKeys._

organization := "com.github.danielwegener"

name := "akka-rtcweb-experimental"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.0")

//fork in Test := true

//parallelExecution in Test := true

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "0.9-SNAPSHOT",
  "com.typesafe.akka" %% "akka-http-core-experimental" % "0.9-SNAPSHOT",
  "com.typesafe.akka" %% "akka-parsing-experimental" % "0.9-SNAPSHOT",
  "com.typesafe.akka" %% "akka-http-experimental" % "0.9-SNAPSHOT",
  "org.typelevel" %% "scodec-core" % "1.3.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % Test,
  "org.scalatest" %% "scalatest" % "2.1.3"   % Test // ApacheV2
)

scalariformSettings

scalaJSSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

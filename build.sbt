organization := "com.github.danielwegener"
name := "akka-rtcweb-experimental"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.4"
crossScalaVersions := Seq("2.10.4", "2.11.4")

//fork in Test := true

//parallelExecution in Test := true

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M1",
  "com.typesafe.akka" %% "akka-parsing-experimental" % "1.0-M1",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M1",
  //"org.typelevel" %% "scodec-core" % "1.3.1",
  "org.typelevel" %% "scodec-core" % "1.5.1-SNAPSHOT",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % Test,
  "org.scalatest" %% "scalatest" % "2.2.1"   % Test // ApacheV2
)

scalariformSettings

//scalaJSSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

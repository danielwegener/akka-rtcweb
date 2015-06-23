organization := "com.github.danielwegener"
name := "akka-rtcweb-experimental"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.6"
crossScalaVersions := Seq("2.10.5", "2.11.6")

//fork in Test := true

//parallelExecution in Test := true

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC3",
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % "1.0-RC3" % "test",
  "com.typesafe.akka" %% "akka-parsing-experimental" % "1.0-RC3",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-RC3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.11",
  "org.scodec" %% "scodec-core" % "1.8.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % Test,
  "org.scalatest" %% "scalatest" % "2.2.5"   % Test, // ApacheV2,
  "org.specs2" %% "specs2-core" % "3.6" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.7" % Test
)

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8",
  "-Xlint",
  "-Xfatal-warnings"
)

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage := Some(url("https://github.com/danielwegener/akka-rtcweb"))

scalariformSettings

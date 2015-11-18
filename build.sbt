organization := "com.github.danielwegener"
name := "akka-rtcweb-experimental"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.10.5", "2.11.7")

//fork in Test := true

//parallelExecution in Test := true

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

lazy val `akka-streams-version` = "2.0-M1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % `akka-streams-version`,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % `akka-streams-version` % "test",
  "com.typesafe.akka" %% "akka-parsing-experimental" % `akka-streams-version`,
  "com.typesafe.akka" %% "akka-http-experimental" % `akka-streams-version`,
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.11",
  "org.scodec" %% "scodec-core" % "1.8.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % Test,
  "org.specs2" %% "specs2-core" % "3.6.2" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.12" % Test
)

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8",
  "-Xlint",
  "-Xfatal-warnings",
  "-target:jvm-1.8",
  "-Ybackend:GenBCode",
  "-Ydelambdafy:method",
  "-Yopt:l:classpath"
)

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage := Some(url("https://github.com/danielwegener/akka-rtcweb"))

scalariformSettings

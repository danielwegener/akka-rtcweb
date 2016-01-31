organization := "com.github.danielwegener"
name := "akka-rtcweb-experimental"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.10.5", "2.11.7")

//fork in Test := true

//parallelExecution in Test := true

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

lazy val akkaVersion = "2.4.2-RC1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-parsing" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "org.scodec" %% "scodec-core" % "1.8.3",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.54",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.54",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.specs2" %% "specs2-core" % "3.6.6" % Test,
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Test
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

organization := "com.github.danielwegener"
name := "akka-rtcweb-experimental"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.6"
crossScalaVersions := Seq("2.10.4", "2.11.4")

//fork in Test := true

//parallelExecution in Test := true

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M4",
  "com.typesafe.akka" %% "akka-parsing-experimental" % "1.0-M4",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M4",
  "org.scodec" %% "scodec-core" % "1.7.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % Test,
  "org.scalatest" %% "scalatest" % "2.2.1"   % Test, // ApacheV2,
  "org.specs2" %% "specs2-core" % "3.6" % Test
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

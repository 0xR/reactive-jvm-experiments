name := "akka-streams-tcp"

version := "1.0"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.26"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

val raft = project.in(file(".")).enablePlugins(JavaAppPackaging)

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "ch.qos.logback" % "logback-classic" % "1.1.7" % "runtime",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test"
)

mainClass in Compile := Some("pl.edu.agh.iosr.raft.RunnerHttp")

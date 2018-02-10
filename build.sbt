name := "effective_akka"

version := "1.0"

scalaVersion := "2.12.4"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.9",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.9" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
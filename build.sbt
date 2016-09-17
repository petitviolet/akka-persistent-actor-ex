name := """akka-actor-persistence-ex"""

version := "1.0.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.9-RC2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,

  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",
  "com.twitter" %% "chill-bijection" % "0.8.0",
//  "com.twitter" %% "chill" % "0.8.0",
//  "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.2.8",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

fork := true
connectInput in run := true

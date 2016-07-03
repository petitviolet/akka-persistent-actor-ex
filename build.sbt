name := """akka-actor-persistence-ex"""

version := "2.4.4"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.4",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.4",
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.4",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",
//  "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.2.8",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

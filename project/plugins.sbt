logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

// Formatter plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// Assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.2")

// Scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-echo" % "0.1.5")

// sbt-dotenv
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "1.1.33")

// Gatling
addSbtPlugin("io.gatling" % "gatling-sbt" % "2.1.0")

// Eclipse
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

// JMH
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.6")

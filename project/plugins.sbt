resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// .env loader
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.146")

// https://github.com/cb372/sbt-explicit-dependencies unusedCompileDependencies, undeclaredCompileDependencies
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.16")

// sbt scalafmt -- Makes our code tidy
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

// sbt coverage test coverageReport
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
// Revolver allows us to use re-start and work a lot faster!
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
// Native Packager allows us to create standalone jar
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")

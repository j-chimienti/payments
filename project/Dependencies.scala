import sbt._

object Dependencies {

  val macwireVersion = "2.3.3"
  val scalaTestVersion = "3.1.1"
  val scalaTestPlayVersion = "4.0.0"
  val akkaVersion = "2.6.18"

  val playJsonV = "2.9.2"
  val macwireV = "2.3.7"
  val scalaTestV = "3.1.1"
  val akkaHttpV = "10.2.4"
  val akkaV = "2.6.15"
  val sttpV = "3.3.11"
  val mongoV = "4.3.0"
  val mockitoScalaV = "1.13.7"
  val guavaV = "28.1-jre"
  val slf4jV = "2.0.0-alpha1"
  val mongoJavaV = "3.6.4"
  val akkaHttpCorsV = "1.0.0"
  lazy val akkaHttpPlayJsonV = "1.28.0"

  val circeConfig = "io.circe" %% "circe-config" % "0.7.0"
  val CatsVersion = "2.2.0"
  val cats = "org.typelevel" %% "cats-core" % CatsVersion

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
  val htmlTags = "com.lihaoyi" %% "scalatags" % "0.9.1"
  lazy val mockito = "org.mockito" %% "mockito-scala" % mockitoScalaV
  lazy val sttpModel = "com.softwaremill.sttp.model" %% "core" % "1.4.7"
  lazy val nameOf = "com.github.dwickern" %% "scala-nameof" % "3.0.0"
  lazy val akkaHttpPlayJson = "de.heikoseeberger" %% "akka-http-play-json" % akkaHttpPlayJsonV
  // http://mongodb.github.io/mongo-java-driver/4.0/driver-scala/tutorials/change-streams/
  lazy val mongo = "org.mongodb.scala" %% "mongo-scala-driver" % mongoV
  lazy val guava = "com.google.guava" % "guava" % guavaV
  lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestV % "test"

  val playJson = "com.typesafe.play" %% "play-json" % playJsonV
  lazy val jsonDeps = Seq(
    playJson,
    "de.heikoseeberger" %% "akka-http-play-json" % "1.31.0"
  )
  lazy val sttp = Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpV,
    "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpV,
    "com.softwaremill.sttp.client3" %% "play-json" % sttpV,
    "com.softwaremill.sttp.client3" %% "slf4j-backend" % sttpV
  )
  lazy val macwire = Seq(
    "com.softwaremill.macwire" %% "macros" % macwireV % "provided",
    "com.softwaremill.macwire" %% "macrosakka" % macwireV % "provided",
    "com.softwaremill.macwire" %% "util" % macwireV,
    "com.softwaremill.macwire" %% "proxy" % macwireV
  )
  lazy val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaV % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
    //    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaV,
    akkaActor,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV
  )
  val circeVersion = "0.12.3"
  val akkaCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.31.0"
  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
  lazy val scalactic = "org.scalactic" %% "scalactic" % scalaTestV
  lazy val bitcoinj = "org.bitcoinj" % "bitcoinj-core" % "0.15.2"
  val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
  val requests = "com.lihaoyi" %% "requests" % "0.6.5"
  lazy val testingDependencies = scalaTest :: mockito :: scalactic :: Nil
  lazy val logging = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    scalaLogging
  )
}

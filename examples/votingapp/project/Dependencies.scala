import sbt._

object dependencies {
  val addResolvers = Seq(
    "52north for postgis".at("http://52north.org/maven/repo/releases/"),
    "jmcardon at bintray".at("https://dl.bintray.com/jmcardon/tsec"),
    Resolver.sonatypeRepo("releases"),
  )

  val exclusions = Seq(
    // see https://github.com/tpolecat/doobie/issues/568
    ExclusionRule("org.typelevel", "scala-library"),
  )

  val compilerPlugins = Seq(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
  )

  val bcrypt = "3.1"
  val cats = "1.4.0"
  val catsMtl = "0.7.1"
  val catsEffect = "0.10.1"
  val circe = "0.9.3"
  val cryptobits = "1.1"
  val doobie = "0.5.3"
  val flyway = "5.0.7"
  val h4sm = "0.0.8"
  val fs2 = "0.10.6"
  val http4s = "0.18.21"
  val logback = "1.2.13"
  val pureConfig = "0.9.2"
  val scalaCheck = "1.14.0"
  val scalaTest = "3.0.8"
  val simulacrum = "0.19.0"
  val tsec = "0.0.1-RC1"

  val httpDeps = Seq(
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-blaze-client",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-dsl",
  ).map(_ % dependencies.http4s) ++ Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-java8",
  ).map(_ % dependencies.circe)

  val testDeps = Seq(
    "org.scalatest" %% "scalatest" % scalaTest,
    "org.tpolecat" %% "doobie-scalatest" % doobie,
    "org.scalacheck" %% "scalacheck" % scalaCheck,
  ).map(_ % "test")

  val dbDeps = Seq(
    "org.flywaydb" % "flyway-core" % flyway,
    "org.postgresql" % "postgresql" % "42.2.2",
  ) ++ Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-hikari",
  ).map(_ % doobie)

  val commonDeps = Seq(
    "com.github.pureconfig" %% "pureconfig" % pureConfig,
    "ch.qos.logback" % "logback-classic" % logback,
    "org.typelevel" %% "cats-core" % cats,
    "org.typelevel" %% "cats-effect" % catsEffect,
    "co.fs2" %% "fs2-core" % fs2,
  )

  val h4smodules = Seq(
    "h4sm-db",
    "h4sm-auth",
    "h4sm-features",
  ).map("com.clovellytech" %% _ % h4sm)
}

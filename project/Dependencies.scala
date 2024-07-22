import sbt._, Keys._
import sbt.librarymanagement.DependencyBuilders
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSCrossVersion
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object dependencies {

  lazy val copyFastOptJS =
    TaskKey[Unit]("copyFastOptJS", "Copy javascript files to target directory")

  val withTests: String = "compile->compile;test->test"
  val inTestOnly: String = "test->test"

  val scala212 = "2.12.12"
  val scala213 = "2.13.3"

  lazy val JsTest = config("js").extend(Test)
  lazy val JvmTest = config("jvm").extend(Test)

  val addResolvers = Seq(
    Resolver.sonatypeRepo("public"),
  )

  object versions {
    val apacheLang3 = "3.15.0"
    val bcrypt = "3.1"
    val betterMonadicFor = "0.3.1"
    val cats = "2.1.1"
    val catsScalacheck = "0.2.0.1"
    val catsMtl = "0.7.1"
    val catsEffect = "2.3.0"
    val circe = "0.13.0"
    val circeConfig = "0.8.0"
    val cryptobits = "1.1"
    val doobie = "0.9.4"
    val flyway = "7.3.2"
    val http4s = "0.21.14"
    val janino = "3.1.2"
    val kindProjector212 = "0.10.3"
    val kindProjector213 = "0.11.0"
    val logback = "1.2.3"
    val macroParadise = "2.1.1"
    val postgres = "42.2.18"
    val scalaCheck = "1.15.2"
    val scalajs = "1.0.0"
    val scalaJavaTime = "2.0.0"
    val scalaTest = "3.2.3"
    val scalaTestPlusScalacheck = "3.2.2.0"
    val simulacrum = "1.0.0"
    val tsec = "0.2.1-ct-2"
  }

  def compilerPlugins = Seq(
    compilerPlugin("com.olegpy" %% "better-monadic-for" % versions.betterMonadicFor),
  )

  def compilerPluginsForVersion(version: String) =
    CrossVersion.partialVersion(version) match {
      case Some((2, major)) if major < 13 =>
        compilerPlugins ++ Seq(
          compilerPlugin(
            ("org.scalamacros" % "paradise" % versions.macroParadise).cross(CrossVersion.full),
          ),
          compilerPlugin("org.typelevel" %% "kind-projector" % versions.kindProjector212),
        )
      case Some((2, major)) if major == 13 =>
        compilerPlugins ++ Seq(
          compilerPlugin("org.typelevel" % s"kind-projector_$version" % versions.kindProjector213),
        )
      case _ => compilerPlugins
    }

  val httpDeps = Seq(
    "http4s-server",
    "http4s-blaze-server",
    "http4s-blaze-client",
    "http4s-circe",
    "http4s-dsl",
  ).map("org.http4s" %% _ % versions.http4s)

  val testDeps = Seq(
    "org.scalatest" %% "scalatest" % versions.scalaTest,
    "org.scalatestplus" %% "scalacheck-1-14" % versions.scalaTestPlusScalacheck,
    "org.tpolecat" %% "doobie-scalatest" % versions.doobie,
    "org.scalacheck" %% "scalacheck" % versions.scalaCheck,
    "com.clovellytech" %% "cats-scalacheck" % versions.catsScalacheck,
  )

  val testDepsInTestOnly = testDeps.map(_ % "test")

  val dbDeps = Seq(
    "org.flywaydb" % "flyway-core" % versions.flyway,
    "org.postgresql" % "postgresql" % versions.postgres,
  ) ++ Seq(
    "doobie-core",
    "doobie-postgres",
    "doobie-hikari",
  ).map("org.tpolecat" %% _ % versions.doobie)

  val commonDeps = Seq(
    "cats-core" -> versions.cats,
    "cats-effect" -> versions.catsEffect,
    "cats-mtl-core" -> versions.catsMtl,
  ).map(("org.typelevel" %% (_: String) % (_: String)).tupled) ++ Seq(
    "org.apache.commons" % "commons-lang3" % versions.apacheLang3,
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "org.codehaus.janino" % "janino" % versions.janino,
    "org.typelevel" %% "simulacrum" % versions.simulacrum,
  ) ++ Seq(
    "circe-core",
    "circe-generic",
    "circe-parser",
//    "circe-java8"
  ).map("io.circe" %% _ % versions.circe) ++ Seq(
    "io.circe" %% "circe-config" % versions.circeConfig,
  )

  val authDeps = Seq(
    "tsec-common",
    "tsec-password",
    "tsec-cipher-jca",
    "tsec-mac",
    "tsec-signatures",
    "tsec-hash-jca",
    "tsec-jwt-mac",
    "tsec-jwt-sig",
    "tsec-http4s",
  ).map("com.clovellytech" %% _ % versions.tsec)
}

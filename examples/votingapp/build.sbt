import dependencies._

organization := "com.clovellytech"

version := Version.version

scalaVersion := Version.scalaVersion

resolvers ++= addResolvers

excludeDependencies ++= exclusions

scalacOptions ++= options.scalac

scalacOptions in (Compile, console) ~= (_.filterNot(options.badScalacConsoleFlags.contains(_)))

libraryDependencies ++= commonDeps ++ http4smodules ++ dbDeps ++ httpDeps ++ testDeps

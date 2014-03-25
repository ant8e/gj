
import scalariform.formatter.preferences._
import AssemblyKeys._

name := "GraphJunkie"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"


scalacOptions += "-feature"

resolvers ++= Dependencies.resolvers

libraryDependencies ++= Dependencies.deps

scalariformSettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveDanglingCloseParenthesis, false)

assemblySettings

mainClass in assembly := Some("gj.Main")

jarName in assembly := "gj.jar"
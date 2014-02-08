
import scalariform.formatter.preferences._

name := "GraphJunkie"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"


scalacOptions += "-feature"

resolvers ++= Dependencies.resolvers

libraryDependencies ++= Dependencies.deps

scalariformSettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveDanglingCloseParenthesis, false)
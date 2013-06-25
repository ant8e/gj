
import scalariform.formatter.preferences._


name := "GraphJunkie"

version := "0.1-SNAPSHOT"


scalaVersion := "2.10.2"

scalacOptions += "-feature"

resolvers ++= Seq(
  "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
   "spray repo" at "http://repo.spray.io/",
     "spray on the edge" at "http://nightlies.spray.io" )

libraryDependencies ++= Dependencies.deps


scalariformSettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference( RewriteArrowSymbols,true)
  .setPreference(PreserveDanglingCloseParenthesis, false)

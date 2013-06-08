//import AssemblyKeys._
import scalariform.formatter.preferences._


name := "GraphJunkie"

version := "0.1-SNAPSHOT"


scalaVersion := "2.10.2"

scalacOptions += "-feature"

resolvers ++= Seq(
  "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
   "spray repo" at "http://repo.spray.io/",
     "spray on the edge" at "http://nightlies.spray.io" )

libraryDependencies ++= Seq ( "io.spray" % "spray-can" % "1.2-20130516",
                   			  "io.spray" % "spray-routing" % "1.2-20130516",
                              "io.spray" %% "spray-json" % "1.2.3",
                              "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1",
							  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
							  "com.typesafe.akka"   %% "akka-testkit" % "2.2.0-RC1" % "test"
                              )



scalariformSettings

//assemblySettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference( RewriteArrowSymbols,true)
  .setPreference(PreserveDanglingCloseParenthesis, false)

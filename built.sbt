//import AssemblyKeys._
import scalariform.formatter.preferences._


name := "GraphJunkie"

version := "0.1-SNAPSHOT"


scalaVersion := "2.10.1"


resolvers ++= Seq(
  "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
   "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= Seq ( "io.spray" 			% "spray-can" % "1.1-M7",
							 "io.spray" 			% "spray-routing" % "1.1-M7",
							 "io.spray" 			%% "spray-json" % "1.2.3",
							 "com.typesafe.akka" 	%% "akka-actor" % "2.1.2" ,
							  "com.typesafe.akka"   %% "akka-testkit" % "2.1.2" % "test",
							 "org.scalatest" %% "scalatest" % "1.9.1" % "test"
                              )



scalariformSettings

//assemblySettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference( RewriteArrowSymbols,true)
  .setPreference(PreserveDanglingCloseParenthesis, false)

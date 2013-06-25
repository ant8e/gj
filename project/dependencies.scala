import sbt._

object Dependencies {

	val sprayVersion = "1.2-M8"
	val akkaVersion ="2.2.0-RC1"

	val	resolvers = Seq(
 			"Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
   			"spray repo" at "http://repo.spray.io/",
     		"spray on the edge" at "http://nightlies.spray.io" )

	val spray  = Seq ( "io.spray" % "spray-can" % sprayVersion,
                   			  "io.spray" % "spray-routing" % sprayVersion,
                   			  "io.spray" %% "spray-json" % "1.2.3")


	val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
	val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
	val scalaTest = "org.scalatest" %% "scalatest" % "1.9.1" 

	val deps = spray ++ Seq (akkaActor, akkaTestKit % "test" , scalaTest % "test")
}
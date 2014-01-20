import sbt._

object Dependencies {

  val sprayVersion = "1.2-RC2"
  val akkaVersion = "2.2.3"

  val resolvers = Seq(
    "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo" at "http://repo.spray.io/",
    "spray on the edge" at "http://nightlies.spray.io",
    "sonatype-snapshots" at "https://oss.sonatype.org/content/groups/public")

  val spray = Seq("io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.2.3")


  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val scalaTest = "org.scalatest" %% "scalatest" % "1.9.1"

  val byteCask = "com.github.bytecask" %% "bytecask" % "1.0-SNAPSHOT"

  val deps =   spray  ++ Seq(byteCask,akkaActor, akkaTestKit % "test", scalaTest % "test")
}
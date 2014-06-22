import sbt._

object Dependencies {

  val sprayVersion = "1.3.1"
  val akkaVersion = "2.3.3"

  val resolvers = Seq(
    "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo" at "http://repo.spray.io/",
    //    "spray on the edge" at "http://nightlies.spray.io",
    "sonatype-snapshots" at "https://oss.sonatype.org/content/groups/public")

  val spray = Seq("io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" % "spray-testkit" % sprayVersion % "test",
    "io.spray" %% "spray-json" % "1.2.6")


  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val scalaTest = "org.scalatest" %% "scalatest" % "1.9.1"

  val byteCask = "com.github.bytecask" %% "bytecask" % "1.0-SNAPSHOT"

  val webJars = Seq("org.webjars" % "jquery" % "1.9.1"
    , "org.webjars" % "highcharts" % "4.0.1",
    "org.webjars" % "highcharts-ng" % "0.0.6",
    "org.webjars" % "highstock" % "1.3.9")


  val deps = spray ++ webJars ++ Seq(akkaActor, byteCask, akkaTestKit % "test", scalaTest % "test")
}
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
    "io.spray" % "spray-caching" % sprayVersion,
    "io.spray" % "spray-testkit" % sprayVersion % "test",
    "io.spray" %% "spray-json" % "1.2.6")


  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1"

  val byteCask = "com.github.bytecask" %% "bytecask" % "1.0-SNAPSHOT"

  val webJars = Seq("org.webjars" % "jquery" % "1.11.1"
    , "org.webjars" % "highcharts" % "4.0.1",
    "org.webjars" % "highstock" % "1.3.9",
    "org.webjars" % "chartjs" % "1.0.1-beta.4",
    "org.webjars" % "bootstrap" % "3.1.1-2",
    "org.webjars" % "angularjs" % "1.2.18",
    "org.webjars" % "underscorejs" % "1.6.0-3"
  )


  val deps = spray ++ webJars ++ Seq(akkaActor, byteCask, akkaTestKit % "test", scalaTest % "test")
}
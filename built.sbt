
import scalariform.formatter.preferences._
import AssemblyKeys._
import DockerKeys._
import sbtdocker.mutable.Dockerfile
import sbtdocker.ImageName

name := "graphjunkie"

lazy val commonSettings = Seq(
version := "0.1-SNAPSHOT",
scalaVersion := "2.11.6",
scalacOptions += "-feature"
) ++ scalariformSettings


//
// Dependencies
//

lazy val root = (project in file("."))
.settings(commonSettings,
  libraryDependencies ++= {
  val sprayVersion = "1.3.3"
  val akkaVersion = "2.3.10"
  Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-caching" % sprayVersion,
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "io.spray" %% "spray-json" % "1.3.1",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.webjars" % "bootstrap" % "3.3.4",
    "org.webjars" % "jquery" % "1.11.1",
    "org.webjars" % "font-awesome" % "4.2.0")}
  ).aggregate(ui)
  .settings((resources in Compile) += (fastOptJS in (ui, Compile)).value.data)
  .settings((resources in Compile) += (fullOptJS in (ui, Compile)).value.data)
  .settings((resources in Compile) += (packageJSDependencies in (ui, Compile)).value)



lazy val ui = (project in file("ui"))
  .settings(
commonSettings,
    persistLauncher := true,
    persistLauncher in Test := false,
    skip in packageJSDependencies := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "com.github.japgolly.scalajs-react" %%% "core" % "0.8.3",
      "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.3",
      "com.lihaoyi" %%% "upickle" % "0.2.8"
    ),
    jsDependencies ++= Seq(
      "org.webjars" % "react" % "0.13.1" / "react-with-addons.js" commonJSName "React",
      "org.webjars" % "rickshaw" % "1.5.0" / "rickshaw.min.js",
      RuntimeDOM % "test"))
  .enablePlugins(ScalaJSPlugin)



Revolver.settings
mainClass in Revolver.reStart := Some("gj.Main")

dockerSettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveDanglingCloseParenthesis, false)

assemblySettings

mainClass in assembly := Some("gj.Main")

jarName in assembly := name.value + "-" + version.value + "-" + readBuildInfoBuildNumber.value + ".jar"


//Docker file definition
docker <<= (docker dependsOn assembly)

imageName in docker := {
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some("b" + readBuildInfoBuildNumber.value))
}

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("dockerfile/java")
    add(artifact, artifactTargetPath)
    expose(8080)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}


// Buildinfo

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber)

buildInfoPackage := "gj.buildinfo"

val readBuildInfoBuildNumber = taskKey[Int]("read buildnumber")

readBuildInfoBuildNumber := {
  val file: File = baseDirectory.value / "buildinfo.properties"
  val prop = new java.util.Properties
  def readProp: Int = {
    prop.load(new java.io.FileInputStream(file))
    prop.getProperty("buildnumber", "0").toInt
  }
  if (file.exists) readProp
  else 0
}
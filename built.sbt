
import scalariform.formatter.preferences._
import AssemblyKeys._
import DockerKeys._
import sbtdocker.mutable.Dockerfile
import sbtdocker.ImageName

name := "graphjunkie"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"


scalacOptions += "-feature"

//
// Dependencies
//

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
    "org.webjars" % "underscorejs" % "1.6.0-3",
    "org.webjars" % "rickshaw" % "1.5.0",
    "org.webjars" % "react" % "0.11.1",
    "org.webjars" % "font-awesome" % "4.2.0"
  )
}

//resolvers ++= Seq(
// "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
// "spray repo" at "http://repo.spray.io/",
//    "spray on the edge" at "http://nightlies.spray.io",
//  "sonatype-snapshots" at "https://oss.sonatype.org/content/groups/public")


scalariformSettings

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
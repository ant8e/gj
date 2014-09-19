
import scalariform.formatter.preferences._
import AssemblyKeys._
import DockerKeys._
import sbtdocker.mutable.Dockerfile
import sbtdocker.ImageName

name := "graphjunkie"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.2"


scalacOptions += "-feature"

resolvers ++= Dependencies.resolvers

libraryDependencies ++= Dependencies.deps

libraryDependencies +=  "org.typelevel" %% "scodec-core" % "1.3.0"

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
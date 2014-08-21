
import scalariform.formatter.preferences._
import AssemblyKeys._
import DockerKeys._
import sbtdocker.mutable.Dockerfile

name := "GraphJunkie"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"


scalacOptions += "-feature"

resolvers ++= Dependencies.resolvers

libraryDependencies ++= Dependencies.deps

scalariformSettings

dockerSettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(PreserveDanglingCloseParenthesis, false)

assemblySettings

mainClass in assembly := Some("gj.Main")

jarName in assembly := "gj.jar"



//Docker file definition
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("dockerfile/java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
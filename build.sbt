Nice.scalaProject

name := "sbt-statika"
description := "Default sbt project settings for statika bundles"
organization := "ohnosequences"

sbtPlugin := true
scalaVersion := "2.10.5"
bucketSuffix := "era7.com"

resolvers += "Github-API" at "http://repo.jenkins-ci.org/public/"


libraryDependencies ++= Seq(
  "ohnosequences" %% "aws-scala-tools" % "0.12.0",
  "ohnosequences" %% "statika" % "2.0.0-SNAPSHOT",
  "ohnosequences" %% "aws-statika" % "2.0.0-SNAPSHOT"
)

// plugins which will be inherrited by anybody who uses this plugin:
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.13.0-SNAPSHOT")
addSbtPlugin("ohnosequences" % "nice-sbt-settings" % "0.6.0-SNAPSHOT")

dependencyOverrides ++= Set(
  "commons-codec" % "commons-codec" % "1.7",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.9.25",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.2",
  "org.scalamacros" % "quasiquotes_2.10" % "2.0.1"
)

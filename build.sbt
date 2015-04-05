Nice.scalaProject

name := "sbt-statika"
description := "Default sbt project settings for statika bundles"
organization := "ohnosequences"

sbtPlugin := true
scalaVersion := "2.10.5"
bucketSuffix := "era7.com"

// plugins which will be inherrited by anybody who uses this plugin:
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.13.0-SNAPSHOT")
addSbtPlugin("ohnosequences" % "nice-sbt-settings" % "0.6.0-SNAPSHOT")

dependencyOverrides += "commons-codec" % "commons-codec" % "1.7"

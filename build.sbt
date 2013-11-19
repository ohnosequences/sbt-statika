Nice.scalaProject

sbtPlugin := true

name := "sbt-statika"

description := "Default sbt project settings for statika bundles"

organization := "ohnosequences"

bucketSuffix := "era7.com"


// plugins which will be inherrited by anybody who uses this plugin:
addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.7.0")

addSbtPlugin("ohnosequences" % "nice-sbt-settings" % "0.3.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

generateDocs := {}

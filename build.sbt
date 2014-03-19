Nice.scalaProject

sbtPlugin := true

name := "sbt-statika"

description := "Default sbt project settings for statika bundles"

organization := "ohnosequences"

bucketSuffix := "era7.com"

resolvers ++= Seq(
  "Era7 maven releases" at "http://releases.era7.com.s3.amazonaws.com"
)

addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.10.1")

addSbtPlugin("ohnosequences" % "nice-sbt-settings" % "0.4.0-RC2")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

dependencyOverrides ++= Set(
  "ohnosequences" % "sbt-s3-resolver" % "0.10.1",
  "com.fasterxml.jackson.core" % "jackson-core"     % "2.2.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.3",
  "commons-codec" % "commons-codec" % "1.7"
)

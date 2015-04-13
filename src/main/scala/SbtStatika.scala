package ohnosequences.sbt

import sbt._, Keys._

import ohnosequences.sbt._
import SbtS3Resolver._, autoImport._
import nice._, NiceProjectConfigs._, ResolverSettings._, AssemblySettings._

object SbtStatikaPlugin extends AutoPlugin {

  object autoImport {

    lazy val statikaVersion = settingKey[String]("Statika library version")
    lazy val awsStatikaVersion = settingKey[String]("AWS-Statika library version")
    lazy val publicResolvers = settingKey[Seq[Resolver]]("Public S3 resolvers for the bundle dependencies")
    lazy val privateResolvers = settingKey[Seq[S3Resolver]]("Private S3 resolvers for the bundle dependencies")
    lazy val fatUrl = settingKey[String]("Fat jsr artifact url")
    lazy val generateMetadata = taskKey[Seq[File]]("Task generating metadata object")
    lazy val metadataObject = settingKey[String]("Name of the generated metadata object")
  }
  import autoImport._

  // This plugin will load automatically
  override def requires = empty
  override def trigger = allRequirements

  // Default settings
  override lazy val projectSettings: Seq[Setting[_]] =
    Nice.scalaProject ++ AssemblySettings.fatArtifactSettings ++ Seq(

    // resolvers needed for statika dependency
    resolvers ++= Seq (
      "Era7 public maven releases"  at s3("releases.era7.com").toHttps(s3region.value.toString),
      "Era7 public maven snapshots" at s3("snapshots.era7.com").toHttps(s3region.value.toString)
    ),

    bucketSuffix := "statika." + organization.value + ".com",

    publicResolvers := Seq(
      Resolver.url("Statika public ivy releases", url(s3("releases."+bucketSuffix.value).toHttps(s3region.value.toString)))(ivy),
      Resolver.url("Statika public ivy snapshots", url(s3("snapshots."+bucketSuffix.value).toHttps(s3region.value.toString)))(ivy)
    ),

    privateResolvers := {
      if (!isPrivate.value) Seq() else Seq(
        s3resolver.value("Statika private ivy releases",  s3("private.releases."+bucketSuffix.value)).withIvyPatterns,
        s3resolver.value("Statika private ivy snapshots", s3("private.snapshots."+bucketSuffix.value)).withIvyPatterns
      )
    },

    // adding privateResolvers to normal ones, if we have credentials
    resolvers ++=
      publicResolvers.value ++
      privateResolvers.value.map{ toSbtResolver(_) },

    // publishing (ivy-style by default)
    publishMavenStyle := false,
    // disable publishing sources and docs
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageDoc) := false,

    statikaVersion := "2.0.0-SNAPSHOT",
    awsStatikaVersion := "2.0.0-SNAPSHOT",

    libraryDependencies += "ohnosequences" %% "statika" % statikaVersion.value,

    libraryDependencies += "ohnosequences" %% "aws-statika" % awsStatikaVersion.value,

    metadataObject := name.value.split("""\W""").map(_.capitalize).mkString,

    // mvn: "[organisation]/[module]_[scalaVersion]/[revision]/[artifact]-[revision]-[classifier].[ext]"
    // ivy: "[organisation]/[module]_[scalaVersion]/[revision]/[type]s/[artifact]-[classifier].[ext]"
    fatUrl := {
      val isMvn = publishMavenStyle.value
      val scalaV = "_"+scalaBinaryVersion.value
      val module = moduleName.value + scalaV
      val artifact =
        (if (isMvn) "" else "jars/") +
        module +
        (if (isMvn) "-"+version.value else "") +
        "-"+fatArtifactClassifier.value +
        ".jar"

      Seq(
        publishS3Resolver.value.url,
        organization.value,
        module,
        version.value,
        artifact
      ).mkString("/")
    },

    generateMetadata := {

      val text = """
        |package generated.metadata
        |
        |import ohnosequences.statika.bundles._
        |
        |case object $metadataObject$ extends AnyArtifactMetadata {
        |  val organization: String = "$organization$"
        |  val artifact:     String = "$artifact$"
        |  val version:      String = "$version$"
        |  val artifactUrl:  String = "$fatUrl$"
        |}
        |""".stripMargin.
          replace("$metadataObject$", metadataObject.value).
          replace("$organization$", organization.value).
          replace("$artifact$", name.value.toLowerCase).
          replace("$version$", version.value).
          replace("$fatUrl$", fatUrl.value)

      val file = (sourceManaged in Compile).value / "statika" / "metadata.scala"
      IO.write(file, text)
      Seq(file)
    },

    sourceGenerators in Compile += generateMetadata.taskValue,

    releaseProcess := constructReleaseProcess(
      initChecks, Seq(
      askVersionsAndCheckNotes,
      //packAndTest,
      genMdDocs,
      genApiDocs,
      publishArtifacts,
      commitAndTag,
      githubRelease,
      nextVersion,
      githubPush
    ))

  )

}

package ohnosequences.sbt

import sbt._, Keys._

import ohnosequences.sbt._
import SbtS3Resolver._, autoImport._
import nice._, NiceProjectConfigs._, ResolverSettings._, AssemblySettings._, ReleaseSettings._
import sbtrelease.ReleasePlugin.ReleaseKeys.releaseProcess

import ohnosequences.statika.bundles._
import ohnosequences.statika.aws._

import ohnosequences.awstools.ec2._
import com.amazonaws.auth.profile._

object SbtStatikaPlugin extends AutoPlugin {

  object autoImport {

    lazy val statikaVersion = settingKey[String]("Statika library version")
    lazy val awsStatikaVersion = settingKey[String]("AWS-Statika library version")
    lazy val publicResolvers = settingKey[Seq[Resolver]]("Public S3 resolvers for the bundle dependencies")
    lazy val privateResolvers = settingKey[Seq[S3Resolver]]("Private S3 resolvers for the bundle dependencies")
    lazy val fatUrl = settingKey[String]("Fat jar artifact url")
    lazy val generateMetadata = taskKey[Seq[File]]("Task generating metadata object")
    lazy val metadataObject = settingKey[String]("Name of the generated metadata object")

    lazy val launchCredentials = settingKey[AWSCredentialsProvider]("Credentials provider for launching an instance")
    lazy val instanceType = settingKey[InstanceType]("Instance type to use for application")
    lazy val keyPair = settingKey[String]("Keypair name for accessing the launched instance")
    lazy val instanceRole = settingKey[Option[String]]("Instance profile role name")
    lazy val applyCompat = settingKey[AnyAMICompatible]("Instance profile role name")
    //lazy val launchedInstances = settingKey[List[]]("Instance profile role name")*/
  }
  import autoImport._

  // This plugin will load automatically
  override def requires = empty
  override def trigger = allRequirements

  val applyCmd: Command = Command.command("applyBundle"){ state: State =>
    import state._
    val extracted = Project.extract(state)
    import extracted._

    val creds = launchCredentials in currentRef get structure.data getOrElse
      sys.error("launchCredentials is not defined")

    val comp = applyCompat in currentRef get structure.data getOrElse
      sys.error("applyCompat is not defined")

    val ami = comp.environment
    val bundle = comp.bundle
    val metadata = comp.metadata

    val instType = instanceType in currentRef get structure.data getOrElse
      sys.error("instanceType is not defined")

    val key = keyPair in currentRef get structure.data getOrElse
      sys.error("keyPair is not defined")

    val role = instanceRole in currentRef get structure.data getOrElse
      sys.error("instanceRole is not defined")

    val specs = InstanceSpecs(
      instanceType = instType,
      amiId = ami.id,
      keyName = key,
      userData = ami.userScript(bundle)(_ => new AMICompatible(ami, bundle, metadata)),
      instanceProfile = role
    )
    println(specs)

    val ec2 = EC2.create(creds)
    ec2.runInstances(1, specs)
    state
  }

  // lazy val applySettings: Seq[Setting[_]] = Seq(

  //   sourceGenerators in Compile += generateApplicator.taskValue
  // )

  // Default settings
  override lazy val projectSettings: Seq[Setting[_]] =
    Nice.scalaProject ++ Seq(

    // resolvers needed for statika dependency
    resolvers ++= Seq (
      "Era7 public maven releases"  at s3("releases.era7.com").toHttps(s3region.value.toString),
      "Era7 public maven snapshots" at s3("snapshots.era7.com").toHttps(s3region.value.toString)
    ),

    bucketSuffix := s"statika.${organization.value}.com",

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
    publishMavenStyle := !isPrivate.value,
    // disable publishing sources and docs
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageDoc) := false,

    statikaVersion := "2.0.0-feature-no-typesets-SNAPSHOT",

    libraryDependencies += "ohnosequences" %% "statika" % statikaVersion.value,

    scalaVersion := "2.10.5",
    crossScalaVersions := Seq("2.11.6", scalaVersion.value),

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
    )),

    commands += applyCmd
  )

  lazy val fatJarSettings: Seq[Setting[_]] = AssemblySettings.fatArtifactSettings ++ Seq(
    awsStatikaVersion := "2.0.0-feature-no-typesets-SNAPSHOT",

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

    sourceGenerators in Compile += generateMetadata.taskValue
  )

}

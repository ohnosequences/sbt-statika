package ohnosequences.sbt

import sbt._
import Keys._

import com.typesafe.sbt.SbtStartScript._

import ohnosequences.sbt.SbtS3Resolver._
import ohnosequences.sbt.NiceSettingsPlugin._
import ohnosequences.sbt.statika.Utils._

object SbtStatikaPlugin extends sbt.Plugin {

  lazy val statikaVersion = settingKey[String]("Statika library version")
  lazy val awsStatikaVersion = settingKey[String]("AWS-Statika library version")
  lazy val publicResolvers = settingKey[Seq[Resolver]]("Public S3 resolvers for the bundle dependencies")
  lazy val privateResolvers = settingKey[Seq[S3Resolver]]("Private S3 resolvers for the bundle dependencies")
  lazy val metadataObject = settingKey[String]("Name of the generated metadata object")

  //////////////////////////////////////////////////////////////////////////////

  object Statika {

    lazy val bundleProject: Seq[Setting[_]] = 
      (startScriptForClassesSettings: Seq[Setting[_]]) ++ 
      (Nice.scalaProject: Seq[Setting[_]]) ++ Seq(

      // resolvers needed for statika dependency
        resolvers ++= Seq ( 
          "Era7 public maven releases"  at toHttp("s3://releases.era7.com")
        , "Era7 public maven snapshots" at toHttp("s3://snapshots.era7.com")
        ) 

      , bucketSuffix := {"statika." + organization.value + ".com"}

      , publicResolvers := Seq(
            Resolver.url("Statika public ivy releases", url(toHttp("s3://releases."+bucketSuffix.value)))(ivy)
          , Resolver.url("Statika public ivy snapshots", url(toHttp("s3://snapshots."+bucketSuffix.value)))(ivy)
          )

      , privateResolvers := {
          if (!isPrivate.value) Seq() else Seq(
              S3Resolver("Statika private ivy releases",  "s3://private.releases."+bucketSuffix.value, ivy)
            , S3Resolver("Statika private ivy snapshots", "s3://private.snapshots."+bucketSuffix.value, ivy)
            )
        }

      // adding privateResolvers to normal ones, if we have credentials
      , resolvers ++= 
            publicResolvers.value ++
          { privateResolvers.value map { r => s3credentials.value map r.toSbtResolver } flatten }

      // publishing (ivy-style by default)
      , publishMavenStyle := false
      // disable publishing sources
      , publishArtifact in (Compile, packageSrc) := false

      , statikaVersion := "1.0.0"
      , awsStatikaVersion := "1.0.0"

      // dependencies
      , libraryDependencies ++= Seq (
          "ohnosequences" %% "statika" % statikaVersion.value
        , "org.scalatest" %% "scalatest" % "2.0" % "test"
        )
      )
  

    lazy val distributionProject: Seq[Setting[_]] = bundleProject ++ Seq(

        libraryDependencies += "ohnosequences" %% "aws-statika" % awsStatikaVersion.value

      // metadata generation
      , metadataObject := name.value.split("""\W""").map(_.capitalize).mkString
      , sourceGenerators in Compile += task[Seq[File]] {
          // helps to serialize Strings correctly:
          def seqToStr(rs: Seq[String]) = 
            if  (rs.isEmpty) "Seq()"  
            else rs.mkString("Seq(\"", "\", \"", "\")")

          // TODO: move it to the sbt-s3-resolver
          def toPublic(r: S3Resolver): Resolver = {
            if(publishMavenStyle.value) r.name at r.url
            else Resolver.url(r.name, url(toHttp(r.url)))(r.patterns)
          }
          // adding publishing resolver to the right list
          val pubResolvers = resolvers.value ++ {
            if(isPrivate.value) Seq() else Seq(toPublic(publishS3Resolver.value))
          }
          val privResolvers = privateResolvers.value ++ {
            if(isPrivate.value) Seq(publishS3Resolver.value) else Seq()
          }

          // Patterns:
          // mvn: "[organisation]/[module]_[scalaVersion]/[revision]/[artifact]-[revision]-[classifier].[ext]"
          // ivy: "[organisation]/[module]_[scalaVersion]/[revision]/[type]s/[artifact]-[classifier].[ext]"
          val fatUrl = {
            val isMvn = publishMavenStyle.value
            val scalaV = "_"+scalaBinaryVersion.value 
            val module = moduleName.value + scalaV
            val artifact = 
              (if (isMvn) "" else "jars/") + 
              module +
              (if (isMvn) "-"+version.value else "") + 
              "-fat.jar"

            Seq( publishS3Resolver.value.url
               , organization.value
               , module
               , version.value
               , artifact
               ).mkString("/")
          }

          val text = """
            |package generated.metadata
            |
            |import ohnosequences.statika.aws._
            |
            |class $metadataObject$(
            |  val organization     : String = "$organization$"
            |, val artifact         : String = "$artifact$"
            |, val version          : String = "$version$"
            |, val resolvers        : Seq[String] = $resolvers$
            |, val privateResolvers : Seq[String] = $privateResolvers$
            |, val artifactUrl      : String = "$fatUrl$"
            |) extends SbtMetadata with FatJarMetadata
            |""".stripMargin.
              replace("$metadataObject$", metadataObject.value).
              replace("$organization$", organization.value).
              replace("$artifact$", name.value.toLowerCase).
              replace("$version$", version.value).
              replace("$resolvers$", seqToStr(pubResolvers map resolverToString flatten)).
              replace("$privateResolvers$", seqToStr(privResolvers map (_.toString))).
              replace("$fatUrl$", fatUrl)

          val file = (sourceManaged in Compile).value / "metadata.scala" 
          IO.write(file, text)
          Seq(file)
        }
      ) ++ Nice.fatArtifactSettings

  }

}

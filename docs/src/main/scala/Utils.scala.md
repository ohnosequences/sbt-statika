
```scala
package ohnosequences.sbt.statika

object Utils {

  import sbt._
  import Keys._

  def seqToString(s: Seq[String]): String = 
    if (s.isEmpty) "Seq()"
    else s.mkString("Seq(\\\"", "\\\", \\\"", "\\\")")

  def patternsToString(ps: Patterns): String =
    "Patterns(%s, %s, %s)" format (
      seqToString(ps.ivyPatterns)
    , seqToString(ps.artifactPatterns)
    , ps.isMavenCompatible
    )

  // TODO: write serializers for the rest of resolvers types
  def resolverToString(r: Resolver): Option[String] = r match {
    case MavenRepository(name: String, root: String) => Some(
      """MavenRepository(\"%s\", \"%s\")""" format (name, root)
      )
    case URLRepository(name: String, patterns: Patterns) => Some(
      """URLRepository(\"%s\", %s)""" format 
        (name, patternsToString(patterns))
      )
    // case ChainedResolver(name: String, resolvers: Seq[Resolver]) => 
    // case FileRepository(name: String, configuration: FileConfiguration, patterns: Patterns) => 
    // case SshRepository(name: String, connection: SshConnection, patterns: Patterns, publishPermissions: Option[String]) => 
    // case SftpRepository(name: String, connection: SshConnection, patterns: Patterns) => 
    case _ => None
  }

}

```


------

### Index

+ src
  + main
    + scala
      + [SbtStatika.scala][main/scala/SbtStatika.scala]
      + [Utils.scala][main/scala/Utils.scala]

[main/scala/SbtStatika.scala]: SbtStatika.scala.md
[main/scala/Utils.scala]: Utils.scala.md
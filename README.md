## Statika sbt-plugin

Default sbt project settings for statika bundles. This plugin is published for sbt `v0.12` and `v0.13`.

### Usage

Add the following dependency to `project/plugins.sbt` file in your sbt project

```scala
resolvers += "Era7 maven releases" at "http://releases.era7.com.s3.amazonaws.com"

addSbtPlugin("ohnosequences" % "sbt-statika" % "1.0.0")
```

### Settings

Here is the list of sbt settings defined by this plugin (see code for defaults):

 Key                 |     Type        | Description                                      
--------------------:|:----------------|:-------------------------------------------------
 `statikaVersion`    | String          | Version of statika library dependency            
 `awsStatikaVersion` | String          | Version of aws-statika library dependency        
 `publicResolvers`   | Seq[Resolver]   | Public S3 resolvers for the bundle dependencies  
 `privateResolvers`  | Seq[S3Resolver] | Private S3 resolvers for the bundle dependencies 
 `metadataObject`    | String          | Name of the generated metadata object            

See also settings from [nice-sbt-settings](https://github.com/ohnosequences/nice-sbt-settings/) plugin.

If you create a bundle, beginning of your `info.sbt` should look like:

```scala
Statika.bundleProject

name := "..."

organization := "..."

// other custom settings
```

If you create a distribution, use `Statika.distributionProject` as the first line instead.


### Dependencies

This plugin adds to your project following dependencies:

* [nice-sbt-settings](https://github.com/ohnosequences/nice-sbt-settings) plugin for standardized release process
* [sbt-start-script](https://github.com/sbt/sbt-start-script) plugin for convenient running
* [scalatest](https://github.com/scalatest/scalatest) library (only for `test` configuration)
* [statika](https://github.com/ohnosequences/statika) library
* [aws-statika](https://github.com/ohnosequences/aws-statika) library (for distributions)
